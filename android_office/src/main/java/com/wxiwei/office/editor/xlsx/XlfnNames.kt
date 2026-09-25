package com.wxiwei.office.editor.xlsx

/**
 * Excel stores functions newer than 2007 with a "_xlfn." prefix; without it Excel shows #NAME?.
 * Typed formulas get the prefix before they are stored, and lose it again for the input box.
 * Quoted strings and sheet names are left untouched.
 */
object XlfnNames {
    private val prefixed = setOf("IFNA", "IFS", "SWITCH", "CONCAT", "TEXTJOIN", "MAXIFS", "MINIFS", "XLOOKUP")
    private val token = Regex("\"(?:\"\"|[^\"])*\"|'(?:''|[^'])*'|(?<![A-Za-z0-9_.])(_xlfn\\.)?([A-Za-z][A-Za-z0-9.]*)(?=\\s*\\()", RegexOption.IGNORE_CASE)

    fun addPrefix(formula: String): String = token.replace(formula) { m ->
        val name = m.groupValues[2]
        if (m.groupValues[1].isEmpty() && name.uppercase() in prefixed) "_xlfn.$name" else m.value
    }

    fun stripPrefix(formula: String): String = token.replace(formula) { m ->
        if (m.groupValues[1].isNotEmpty()) m.groupValues[2] else m.value
    }
}
