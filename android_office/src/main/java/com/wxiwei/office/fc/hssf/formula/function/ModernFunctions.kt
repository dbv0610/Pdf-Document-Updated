package com.wxiwei.office.fc.hssf.formula.function

import com.wxiwei.office.fc.hssf.formula.OperationEvaluationContext
import com.wxiwei.office.fc.hssf.formula.TwoDEval
import com.wxiwei.office.fc.hssf.formula.eval.AreaEval
import com.wxiwei.office.fc.hssf.formula.eval.BlankEval
import com.wxiwei.office.fc.hssf.formula.eval.BoolEval
import com.wxiwei.office.fc.hssf.formula.eval.ErrorEval
import com.wxiwei.office.fc.hssf.formula.eval.EvaluationException
import com.wxiwei.office.fc.hssf.formula.eval.MissingArgEval
import com.wxiwei.office.fc.hssf.formula.eval.NumberEval
import com.wxiwei.office.fc.hssf.formula.eval.OperandResolver
import com.wxiwei.office.fc.hssf.formula.eval.RefEval
import com.wxiwei.office.fc.hssf.formula.eval.StringEval
import com.wxiwei.office.fc.hssf.formula.eval.ValueEval
import com.wxiwei.office.fc.hssf.formula.udf.DefaultUDFFinder
import com.wxiwei.office.fc.hssf.formula.udf.UDFFinder

/**
 * Excel 2007+ functions the bundled POI engine lacks. In XLSX they are stored as plain names
 * (IFERROR, SUMIFS...) or with the "_xlfn." prefix (IFNA, XLOOKUP...); the formula parser turns
 * both into user-defined-function calls that [finder] resolves.
 *
 * Arguments arrive pre-evaluated (no short-circuit), which only costs time: none has side effects.
 * XLOOKUP cannot spill, so a multi-column return array yields its first column.
 */
object ModernFunctions {
    private val functions: Map<String, FreeRefFunction> = linkedMapOf(
        "IFERROR" to FreeRefFunction { args, ec -> ifError(args, ec) { true } },
        "IFNA" to FreeRefFunction { args, ec -> ifError(args, ec) { it.errorCode == ErrorEval.NA.errorCode } },
        "IFS" to FreeRefFunction(::ifs),
        "SWITCH" to FreeRefFunction(::switch),
        "CONCAT" to FreeRefFunction(::concat),
        "TEXTJOIN" to FreeRefFunction(::textJoin),
        "SUMIFS" to FreeRefFunction { args, ec -> aggregateIfs(args, ec, valueRange = true, Agg.SUM) },
        "COUNTIFS" to FreeRefFunction { args, ec -> aggregateIfs(args, ec, valueRange = false, Agg.COUNT) },
        "AVERAGEIFS" to FreeRefFunction { args, ec -> aggregateIfs(args, ec, valueRange = true, Agg.AVERAGE) },
        "MAXIFS" to FreeRefFunction { args, ec -> aggregateIfs(args, ec, valueRange = true, Agg.MAX) },
        "MINIFS" to FreeRefFunction { args, ec -> aggregateIfs(args, ec, valueRange = true, Agg.MIN) },
        "AVERAGEIF" to FreeRefFunction(::averageIf),
        "XLOOKUP" to FreeRefFunction(::xlookup),
    )

    /** Upper-case names this finder implements, without the "_xlfn." prefix. */
    val names: Set<String> get() = functions.keys

    val finder: UDFFinder = DefaultUDFFinder(functions.keys.toTypedArray(), functions.values.toTypedArray())

    // region helpers

    private class Failure(val error: ErrorEval) : Exception(null, null, false, false)

    private fun fail(error: ErrorEval): Nothing = throw Failure(error)

    private inline fun guarded(block: () -> ValueEval): ValueEval = try {
        block()
    } catch (e: Failure) {
        e.error
    } catch (e: EvaluationException) {
        e.errorEval
    }

    /** Single value of [arg] (implicit intersection for ranges); errors come back as ErrorEval. */
    private fun single(arg: ValueEval, ec: OperationEvaluationContext): ValueEval = try {
        OperandResolver.getSingleValue(arg, ec.rowIndex, ec.columnIndex)
    } catch (e: EvaluationException) {
        e.errorEval
    }

    private fun area(arg: ValueEval): TwoDEval = when (arg) {
        is TwoDEval -> arg
        is RefEval -> arg.offset(0, 0, 0, 0)
        is ErrorEval -> fail(arg)
        else -> fail(ErrorEval.VALUE_INVALID)
    }

    private fun isMissing(arg: ValueEval) = arg === MissingArgEval.instance

    /** Every cell value of [arg] in row-major order (a scalar yields itself). */
    private fun flatten(arg: ValueEval): Sequence<ValueEval> = when (arg) {
        is TwoDEval -> sequence {
            for (r in 0 until arg.height) for (c in 0 until arg.width) yield(arg.getValue(r, c))
        }
        is RefEval -> sequenceOf(arg.innerValueEval)
        else -> sequenceOf(arg)
    }

    private fun text(value: ValueEval): String = when (value) {
        is ErrorEval -> fail(value)
        BlankEval.instance, MissingArgEval.instance -> ""
        else -> OperandResolver.coerceValueToString(value)
    }

    private fun bool(value: ValueEval): Boolean {
        if (value is ErrorEval) fail(value)
        return OperandResolver.coerceValueToBoolean(value, false) ?: false
    }

    /** Excel's "=" for SWITCH/XLOOKUP: same type, text compared case-insensitively. */
    private fun sameValue(a: ValueEval, b: ValueEval): Boolean = when {
        a is NumberEval && b is NumberEval -> a.numberValue == b.numberValue
        a is StringEval && b is StringEval -> a.stringValue.equals(b.stringValue, ignoreCase = true)
        a is BoolEval && b is BoolEval -> a.booleanValue == b.booleanValue
        else -> false
    }

    /** Orders same-typed values (numbers < text < booleans, as Excel sorts); null when not comparable. */
    private fun compare(a: ValueEval, b: ValueEval): Int? = when {
        a is NumberEval && b is NumberEval -> a.numberValue.compareTo(b.numberValue)
        a is StringEval && b is StringEval -> a.stringValue.compareTo(b.stringValue, ignoreCase = true)
        a is BoolEval && b is BoolEval -> a.booleanValue.compareTo(b.booleanValue)
        else -> null
    }

    // endregion

    private fun ifError(args: Array<ValueEval>, ec: OperationEvaluationContext, catches: (ErrorEval) -> Boolean): ValueEval {
        if (args.size != 2) return ErrorEval.VALUE_INVALID
        val value = single(args[0], ec)
        if (value is ErrorEval && catches(value)) {
            return single(args[1], ec).let { if (it === BlankEval.instance) NumberEval.ZERO else it }
        }
        return if (value === BlankEval.instance) NumberEval.ZERO else value
    }

    private fun ifs(args: Array<ValueEval>, ec: OperationEvaluationContext): ValueEval = guarded {
        if (args.isEmpty() || args.size % 2 != 0) fail(ErrorEval.VALUE_INVALID)
        for (i in args.indices step 2) {
            if (bool(single(args[i], ec))) return@guarded single(args[i + 1], ec)
        }
        ErrorEval.NA
    }

    private fun switch(args: Array<ValueEval>, ec: OperationEvaluationContext): ValueEval = guarded {
        if (args.size < 3) fail(ErrorEval.VALUE_INVALID)
        val expr = single(args[0], ec)
        if (expr is ErrorEval) return@guarded expr
        var i = 1
        while (i + 1 < args.size) {
            val candidate = single(args[i], ec)
            if (candidate is ErrorEval) return@guarded candidate
            if (sameValue(expr, candidate)) return@guarded single(args[i + 1], ec)
            i += 2
        }
        if (i < args.size) single(args[i], ec) else ErrorEval.NA
    }

    private fun concat(args: Array<ValueEval>, ec: OperationEvaluationContext): ValueEval = guarded {
        val out = StringBuilder()
        for (arg in args) for (value in flatten(arg)) out.append(text(value))
        if (out.length > 32767) fail(ErrorEval.VALUE_INVALID)
        StringEval(out.toString())
    }

    private fun textJoin(args: Array<ValueEval>, ec: OperationEvaluationContext): ValueEval = guarded {
        if (args.size < 3) fail(ErrorEval.VALUE_INVALID)
        val delimiters = flatten(args[0]).map(::text).toList().ifEmpty { listOf("") }
        val ignoreEmpty = bool(single(args[1], ec))
        val parts = args.drop(2).flatMap { flatten(it).map(::text).toList() }
            .filter { !ignoreEmpty || it.isNotEmpty() }
        val out = StringBuilder()
        parts.forEachIndexed { index, part ->
            if (index > 0) out.append(delimiters[(index - 1) % delimiters.size])
            out.append(part)
        }
        if (out.length > 32767) fail(ErrorEval.VALUE_INVALID)
        StringEval(out.toString())
    }

    private enum class Agg { SUM, COUNT, AVERAGE, MAX, MIN }

    /**
     * SUMIFS / COUNTIFS / AVERAGEIFS / MAXIFS / MINIFS: [valueRange] says whether args[0] is the
     * range to aggregate; the rest are (criteria range, criteria) pairs of the same size.
     */
    private fun aggregateIfs(args: Array<ValueEval>, ec: OperationEvaluationContext, valueRange: Boolean, agg: Agg): ValueEval = guarded {
        val first = if (valueRange) 1 else 0
        if (args.size - first < 2 || (args.size - first) % 2 != 0) fail(ErrorEval.VALUE_INVALID)
        val values = if (valueRange) area(args[0]) else null
        val ranges = ArrayList<TwoDEval>()
        val predicates = ArrayList<CountUtils.I_MatchPredicate?>()
        for (i in first until args.size step 2) {
            ranges += area(args[i])
            predicates += Countif.createCriteriaPredicate(args[i + 1], ec.rowIndex, ec.columnIndex)
        }
        val height = values?.height ?: ranges[0].height
        val width = values?.width ?: ranges[0].width
        if (ranges.any { it.height != height || it.width != width }) fail(ErrorEval.VALUE_INVALID)
        aggregate(height, width, agg, { r, c -> values?.getValue(r, c) }) { r, c ->
            ranges.indices.all { k -> predicates[k]?.matches(ranges[k].getValue(r, c)) == true }
        }
    }

    /** AVERAGEIF(range, criteria, [average_range]): average_range takes the size of range. */
    private fun averageIf(args: Array<ValueEval>, ec: OperationEvaluationContext): ValueEval = guarded {
        if (args.size !in 2..3) fail(ErrorEval.VALUE_INVALID)
        val range = area(args[0])
        val predicate = Countif.createCriteriaPredicate(args[1], ec.rowIndex, ec.columnIndex)
        val values = if (args.size == 3 && !isMissing(args[2])) area(args[2]) else range
        val sized = if (values is AreaEval) values.offset(0, range.height - 1, 0, range.width - 1) else values
        aggregate(range.height, range.width, Agg.AVERAGE, { r, c -> sized.getValue(r, c) }) { r, c ->
            predicate?.matches(range.getValue(r, c)) == true
        }
    }

    private inline fun aggregate(
        height: Int, width: Int, agg: Agg,
        valueAt: (Int, Int) -> ValueEval?, matches: (Int, Int) -> Boolean
    ): ValueEval {
        var count = 0
        var sum = 0.0
        var best: Double? = null
        for (r in 0 until height) for (c in 0 until width) {
            if (!matches(r, c)) continue
            if (agg == Agg.COUNT) { count++; continue }
            val value = valueAt(r, c)
            if (value is ErrorEval) fail(value)
            // Like Excel: only real numbers count; text, booleans and blanks are skipped.
            val number = (value as? NumberEval)?.numberValue ?: continue
            count++
            sum += number
            best = when {
                best == null -> number
                agg == Agg.MAX -> maxOf(best, number)
                else -> minOf(best, number)
            }
        }
        return when (agg) {
            Agg.COUNT -> NumberEval(count.toDouble())
            Agg.SUM -> NumberEval(sum)
            Agg.AVERAGE -> if (count == 0) ErrorEval.DIV_ZERO else NumberEval(sum / count)
            Agg.MAX, Agg.MIN -> NumberEval(best ?: 0.0)
        }
    }

    /** XLOOKUP(lookup, lookup_array, return_array, [if_not_found], [match_mode], [search_mode]). */
    private fun xlookup(args: Array<ValueEval>, ec: OperationEvaluationContext): ValueEval = guarded {
        if (args.size !in 3..6) fail(ErrorEval.VALUE_INVALID)
        val key = single(args[0], ec)
        if (key is ErrorEval) return@guarded key
        val lookup = area(args[1])
        val results = area(args[2])
        fun optionalInt(index: Int, default: Int): Int =
            if (args.size <= index || isMissing(args[index])) default
            else OperandResolver.coerceValueToInt(single(args[index], ec).also { if (it is ErrorEval) fail(it) })
        val matchMode = optionalInt(4, 0)
        val searchMode = optionalInt(5, 1)
        if (matchMode !in -1..2 || searchMode !in listOf(1, -1, 2, -2)) fail(ErrorEval.VALUE_INVALID)
        val vertical = lookup.width == 1
        if (!vertical && lookup.height != 1) fail(ErrorEval.VALUE_INVALID)
        val length = if (vertical) lookup.height else lookup.width
        if ((if (vertical) results.height else results.width) != length) fail(ErrorEval.VALUE_INVALID)
        val wildcard = if (matchMode == 2 && key is StringEval) wildcardRegex(key.stringValue) else null
        // Binary search modes (±2) assume sorted data; a linear scan gives the same answer.
        val order = if (searchMode < 0) (length - 1 downTo 0) else (0 until length)
        var found = -1
        var nearest: ValueEval? = null
        for (i in order) {
            val candidate = if (vertical) lookup.getValue(i, 0) else lookup.getValue(0, i)
            if (wildcard != null) {
                if (candidate !is BlankEval && wildcard.matches(text(candidate))) { found = i; break }
                continue
            }
            if (sameValue(key, candidate)) { found = i; break }
            if (matchMode == -1 || matchMode == 1) {
                val cmp = compare(candidate, key) ?: continue
                val better = nearest?.let { compare(candidate, it) } ?: 0
                val qualifies = if (matchMode == -1) cmp < 0 && (nearest == null || better > 0)
                else cmp > 0 && (nearest == null || better < 0)
                if (qualifies) { nearest = candidate; found = i }
            }
        }
        if (found < 0) {
            return@guarded if (args.size > 3 && !isMissing(args[3])) single(args[3], ec) else ErrorEval.NA
        }
        val value = if (vertical) results.getValue(found, 0) else results.getValue(0, found)
        if (value === BlankEval.instance) NumberEval.ZERO else value
    }

    /** Excel wildcards: * any run, ? one char, ~ escapes the next; case-insensitive. */
    private fun wildcardRegex(pattern: String): Regex {
        val out = StringBuilder()
        var i = 0
        while (i < pattern.length) {
            val ch = pattern[i]
            when {
                ch == '~' && i + 1 < pattern.length -> { out.append(Regex.escape(pattern[i + 1].toString())); i++ }
                ch == '*' -> out.append(".*")
                ch == '?' -> out.append('.')
                else -> out.append(Regex.escape(ch.toString()))
            }
            i++
        }
        return Regex(out.toString(), setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    }
}
