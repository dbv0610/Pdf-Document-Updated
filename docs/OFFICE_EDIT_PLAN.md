# Kế hoạch chỉnh sửa tài liệu (PDF / DOCX / PPTX / XLSX) — module `android_office`

Ngày: 2026-09-25 · Nhánh: `fix-eror-debug`

## 1. Kết luận khả thi

| Định dạng | Khả thi? | Cách làm | Ghi chú |
|---|---|---|---|
| **PDF** | ✅ Cao | pdfium (`libpdfium.so` prebuilt) đã export đủ `fpdf_edit`/`fpdf_annot`/`fpdf_save`. Đã có sẵn: chọn text, underline/strikeout, ink + undo/redo, `saveDocument()`. | Chỉ cần thêm highlight, text, ảnh, xoá/hit-test annotation. Text dùng font TTF (`/system/fonts`) để có tiếng Việt. |
| **DOCX** | ✅ Trung bình | Viewer là **read-only** (reader → model → view, không có writer). Làm theo kiểu **"patch OOXML"**: sửa `word/document.xml` bằng dom4j, ghi zip mới, **mở lại** file để render. | Cần thêm "source map" trong `DOCXReader` để map offset chọn text (`Highlight.getSelectStart/End`) → `w:p`/`w:r` trong XML. Chỉ sửa phần thân (MAIN), không sửa header/footer/textbox ở v1. |
| **PPTX** | ✅ Trung bình | Patch OOXML: thêm `p:sp` (textbox) / `p:pic` (ảnh) vào `ppt/slides/slideN.xml`, sửa text shape theo `cNvPr id`, lưu rồi mở lại. | |
| **XLSX** | ✅ Trung bình | Patch `xl/worksheets/sheetN.xml` (giá trị + `<f>`). Tính công thức: engine POI có sẵn trong `fc/hssf/formula` (`WorkbookEvaluator`, 79 hàm) — viết adapter `EvaluationWorkbook` trên model `ss`. Sửa ô hiển thị ngay (in-memory) + lưu file. | Hiện `CellReader` **bỏ `<f>`**, chỉ đọc giá trị cache `<v>` → phải giữ lại formula. |
| .doc / .ppt / .xls (nhị phân) | ❌ | Không có writer nhị phân; không làm. | UI nên ẩn nút edit, hoặc gợi ý "lưu thành PDF". |

**Tại sao DOCX/PPTX dùng "sửa XML → lưu → mở lại" thay vì sửa trực tiếp trên view?** Model layout của wp/pg phụ thuộc offset toàn cục và cache layout; sửa trực tiếp rất dễ vỡ. Mở lại file cho kết quả render đúng 100% như file thật, và file lưu ra mở được bằng Word/PowerPoint. Mất ~0.5–1.5 s mỗi lần áp dụng → UI nên gom nhiều thao tác rồi "Áp dụng" một lần, và khôi phục vị trí cuộn/trang sau khi mở lại.

## 2. Kiến trúc

```
android_office/src/main/java/com/wxiwei/office/editor/
  EditResult.kt              // sealed: Ok(file) | Error(reason, message)
  ooxml/OoxmlPackage.kt      // đọc zip vào bộ nhớ, get/put XML (dom4j), addMedia(+rels +[Content_Types]), saveAtomic(target)
  docx/DocxSourceMap.kt      // do DOCXReader ghi: paragraph/run ↔ offset
  docx/DocxEditor.kt
  pptx/PptxEditor.kt
  xlsx/XlsxFormulaEngine.kt  // adapter EvaluationWorkbook trên ss.model
  xlsx/XlsxEditor.kt
com/reader/pdfviewer/        // PDF: thêm API vào PDFView (như ink)
app/src/dev/...              // "Edit Lab" — màn hình test chỉ có trong flavor dev
```

Nguyên tắc chung:
- Engine **không có UI**. App (anh) tự làm UI; Edit Lab chỉ để test.
- Mọi hàm ghi file: ghi vào file tạm rồi rename (không bao giờ làm hỏng file gốc). Mặc định lưu ra file mới.
- Hàm trả `EditResult`, không throw ra ngoài.
- File mã hoá (enc_*): ghi ra bản **không mã hoá** + trả cờ cảnh báo (v1).

## 3. API dự kiến (cho anh làm UI)

### PDF (`PDFView`)
```kotlin
addTextMarkupToSelection(TextMarkupType.HIGHLIGHT | UNDERLINE | STRIKETHROUGH, color)   // bôi đen → highlight
addText(page: Int, pageX: Float, pageY: Float, text: String, sizePt: Float, @ColorInt color: Int): String?  // trả annotation name
addImage(page: Int, pageRect: RectF, bitmap: Bitmap): String?
viewToPagePoint(viewX, viewY): Pair<Int, PointF>?        // đổi toạ độ chạm → toạ độ trang
findAnnotationAt(viewX, viewY): PdfAnnotationInfo?        // page, name, subtype, rect — để chọn/xoá
removeAnnotation(page, name): Boolean
undoEdit()/redoEdit()/canUndoEdit()/canRedoEdit()        // gộp chung ink + markup + text + ảnh
saveDocument(file)                                        // có sẵn
```

### DOCX (`DocxEditor(sourceFile, sourceMap)`)
Offset lấy từ selection của Word view: `word.getHighlight().getSelectStart()/getSelectEnd()`.
```kotlin
highlight(start, end, color: String = "yellow")
setBold/ setItalic/ setUnderline(start, end, on: Boolean)
setTextColor(start, end, rgbHex)
insertText(offset, text) ; deleteText(start, end) ; replaceText(start, end, text)
insertImage(offset, imageFile, widthPx, heightPx)        // inline drawing
appendParagraph(text)
save(target: File): EditResult                           // sau đó mở lại target
```

### PPTX (`PptxEditor(sourceFile)`)
```kotlin
addTextBox(slideIndex, rectEmu, text, sizePt, rgbHex): Int /*shapeId*/
addImage(slideIndex, rectEmu, imageFile): Int
setShapeText(slideIndex, shapeId, text)
moveShape(slideIndex, shapeId, rectEmu) ; deleteShape(slideIndex, shapeId)
listShapes(slideIndex): List<PptxShapeInfo>   // id, name, rect, text — để UI hit-test
save(target)
```

### PPTX realtime (E6, khuyên dùng thay cho PptxEditor khi đang mở file)
```kotlin
val live = LivePptxSession(control, sourceFile)          // control = IControl của file đang mở
live.addTextBox(slide, rectEmu, "text", 24f, "0000FF")   // hiện ngay trên slide
live.addImage(slide, rectEmu, pngFile); live.setShapeText(slide, id, "...")
live.moveShape(slide, id, rectEmu); live.deleteShape(slide, id)
live.undo(); live.redo(); live.listener = LivePptxSession.OnChangeListener { canUndo, canRedo -> }
live.save(target)                                         // ghi file, KHÔNG cần mở lại
// Toạ độ chạm → EMU: SlideGeometry.viewToEmu(presentation, x, y); chọn shape: SlideGeometry.hitTest(live.listShapes(slide), point)
```

### XLSX
```kotlin
// Sửa trực tiếp trên view đang mở (hiển thị ngay):
SheetEditSession(control).setCellValue(sheet, row, col, "123" | "abc" | "=SUM(A1:A3)")  // tự tính lại các ô phụ thuộc, repaint
SheetEditSession.save(target)   // patch XLSX gốc: ghi <v>, <f>, xoá calcChain.xml, bật fullCalcOnLoad
// Hoặc offline:
XlsxEditor(sourceFile).setCell(...).save(target)
```

## 4. Các bước (giao Codex, mỗi bước 1 lane)

Mỗi bước: script `AS005-lanes/codex-edit-eN.sh`, prompt `prompt-edit-eN.txt`, tiến độ `edit-eN-progress.txt`, xong tạo `edit-eN-DONE`. Claude review + build + test trên máy sau mỗi bước, anh test rồi mới sang bước sau.

| Bước | Nội dung | Test |
|---|---|---|
| **E1** | PDF: HIGHLIGHT, addText (FreeText + font TTF), addImage (Stamp + image object), hit-test/xoá annotation, undo/redo chung | Edit Lab + mở file lưu ra bằng app khác |
| **E2** | `OoxmlPackage` + `EditResult` + JVM unit test (zip/xml/rels/content types) | `gradlew :android_office:testDebugUnitTest` |
| **E3** | DOCX: source map trong DOCXReader + `DocxEditor` | Edit Lab: highlight/insert/ảnh → mở lại |
| **E4** | PPTX: `PptxEditor` + shape id trong model pg | Edit Lab |
| **E5** | XLSX: giữ `<f>` trong CellReader, `XlsxFormulaEngine`, `SheetEditSession` (sửa ô + tính lại + repaint), `XlsxEditor.save` | Edit Lab + mở bằng Excel/Sheets |
| **E6** | Edit Lab (flavor dev): danh sách file trong /sdcard/Documents, nút cho từng thao tác, lưu ra `/sdcard/Documents/edited/`, mở kết quả trong viewer | Trên máy |

E6 làm song song dần: mỗi bước E1–E5 thêm nút của mình vào Edit Lab.

## 5. Rủi ro / giới hạn v1
- DOCX: source map phải khớp chính xác cách `DOCXReader` đếm offset (tab, field, ký tự đặc biệt). Editor kiểm tra lại text của đoạn trước khi sửa; lệch → trả `Error(MAP_MISMATCH)` chứ không ghi bừa.
- DOCX/PPTX: không hỗ trợ sửa text trong textbox/SmartArt/header/footer ở v1.
- XLSX: hàm không có trong engine POI → giữ giá trị cache cũ + `fullCalcOnLoad` để Excel tự tính lại.
- PDF text: là annotation (có thể xoá/di chuyển), không sửa được text gốc có sẵn trong PDF.


---

## 6. Trạng thái & bàn giao (cập nhật 2026-09-25)

| Bước | Trạng thái | Ghi chú |
|---|---|---|
| E1 PDF | ✅ xong, test máy | highlight/text/ảnh/xoá/undo/redo/lưu (`PDFView`) |
| E2 OOXML | ✅ | `editor/ooxml/OoxmlPackage` |
| E3 DOCX (lưu + mở lại) | ✅ | `DocxEditor`, `WordSelection.offsetAtScreen(rawX, rawY)` |
| E4 PPTX (lưu + mở lại) | ✅ | `PptxEditor` (giữ package trong RAM, `undoLast()`) |
| E6 PPTX realtime | ✅ xong, test máy | `LivePptxSession` — xem mục 3 |
| E5 XLSX sửa ô + công thức realtime | ✅ phần công thức xong, test máy | xem dưới |
| E5b XLSX format style (đậm, màu, nền, định dạng số, căn lề) | ⏳ chưa làm | user yêu cầu, để lần sau |
| E7 DOCX realtime | ⏳ chưa làm | prompt sẵn: `AS005-lanes/prompt-edit-e7.txt` |

### XLSX — đã làm (E5)
- Đọc giữ công thức: `Cell.formula` (CellReader + SheetReader, shared formula dịch bằng `A1FormulaShifter`).
- `editor/xlsx/XlsxEvaluationWorkbook` — adapter engine POI (fc/hssf/formula) cho model XLSX; cột nguyên (`$E:$E`) được cắt tới dòng cuối có dữ liệu.
- `XlsxFormulaEngine` — tính lại **chỉ các ô phụ thuộc** (chỉ mục phụ thuộc + BFS), `recalc()` = tính tất cả (dùng để kiểm tra).
- `SheetEditSession(control, file)` — API cho UI:
  ```kotlin
  val s = SheetEditSession(control, file)
  s.setCellInput(sheetIndex, row, col, "=SUM(A1:A5)")  // hoặc "123", "TRUE", "chữ", "" (xoá)
  s.getInput(sheetIndex, row, col)                    // "=SUM(A1:A5)" để hiện trong ô nhập
  s.undo(); s.redo(); s.warnings; s.lastError
  s.save(target)                                      // patch file gốc, không cần mở lại
  // ô đang chọn: spreadsheet.getSheetView().getCurrentSheet().getActiveCellRow()/Column()
  ```
- `XlsxWriter` — chỉ sửa ô bị đổi (giữ style `s=`), mở rộng shared formula khi ghi đè ô master, xoá `calcChain.xml`, bật `fullCalcOnLoad`.
- Sửa lỗi sẵn có của module: `functionMetadata.txt` nằm sai thư mục (src/main/java → src/main/resources) nên trước đây KHÔNG parse được công thức nào; `WorkbookEvaluator` ép kiểu cứng sang `ACell` (.xls); `TEXT()` hiểu "mm" thành phút; `UDFFinder.DEFAULT` tự chứa chính nó (đệ quy vô hạn).
- Kiểm chứng trên máy: `Bao_cao_phan_tich_task_H1_2026.xlsx` 776 công thức → **0 sai lệch** so với giá trị Excel lưu; `Azura - Order Product Master.xlsx` 56k công thức → 0 ô bị ghi sai; sửa ô: lần đầu ~4 s (dựng chỉ mục), sau đó 30–750 ms. 51+ unit test xanh (`XlsxEditTest`).

### XLSX — việc tiếp theo (theo thứ tự)
1. ✅ **Hàm mới (2026-09-26)**: `IFERROR`, `IFNA`, `SUMIFS`, `COUNTIFS`, `AVERAGEIF(S)`, `MAXIFS/MINIFS`, `XLOOKUP` (match mode 0/-1/1/2, search ±1), `CONCAT`, `TEXTJOIN`, `IFS`, `SWITCH` — `fc/hssf/formula/function/ModernFunctions.kt`, nối qua `XlsxEvaluationWorkbook.getNameXPtg/resolveNameXText` (bỏ `_xlfn.`). Công thức gõ vào được thêm `_xlfn.` khi lưu (`XlfnNames`), ô nhập hiển thị không có tiền tố. `WorkbookEvaluator` không còn ép kiểu `HSSFEvaluationWorkbook` khi gặp `NameXPtg`.
   - **Tăng tốc lookup**: MATCH/VLOOKUP/HLOOKUP khớp chính xác dùng chỉ mục cột (`ExactLookupIndexes`, chỉ bật trong engine XLSX, xoá khi sửa ô). Azura recalc toàn bộ: 500 s → 39 s.
   - **Sheet chưa load**: công thức đọc sheet chưa load (viewer load sheet lười) giờ giữ giá trị cũ + cảnh báo thay vì tính trên ô trống.
   - Azura sau khi sửa: 135 ô khác giá trị lưu, đều do dữ liệu (file xuất từ Google Sheets: khoá tra cứu `\r\n` vs `\n`, giá trị lưu cũ nằm ngoài vùng công thức) — engine tính đúng theo dữ liệu. Lab ghi đủ danh sách vào `files/recalc.tsv` (`adb shell run-as com.azg.pdf8 cat files/recalc.tsv`).
   - Còn thiếu: MATCH/VLOOKUP với ký tự đại diện `*`/`?` (POI ném "Wildcard ... not supported").
2. **Dựng chỉ mục phụ thuộc ở nền** ngay sau khi mở file (hiện lần sửa đầu mất ~4 s trên file 56k công thức). Lưu ý: model chỉ an toàn trên UI thread → parse ở nền, gắn kết quả trên UI thread.
3. **E5b format style**: bold/italic/màu chữ/nền/viền/định dạng số/căn lề cho ô hoặc vùng chọn. Live: sửa `CellStyle` (ss/model/style) + `SheetView.invalidateTiles()`. Lưu: thêm `xf` mới vào `xl/styles.xml` (fonts/fills/borders/numFmts/cellXfs) rồi đặt `s=` cho ô trong `XlsxWriter`.
4. Tên vùng (defined names) chưa hỗ trợ → `#NAME?` giữ giá trị cũ.

### Cách test nhanh (dev flavor)
- `adb shell am start -n com.azg.pdf8/com.azg.pdf8.editlab.EditLabActivity` → nút DOCX / PPTX / XLSX.
- XLSX: `...XlsxEditLabActivity --es file Ten_file.xlsx`; chạm ô → nhập → Set; "Recalc check" so engine với giá trị Excel; log tag `XLSXLAB`.
- Script điều khiển máy: scratchpad `lab.sh` (press/typein/st) — chú ý bàn phím ảo làm lệch layout, luôn ẩn bàn phím trước khi chạm vào sheet.
