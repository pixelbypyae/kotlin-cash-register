
/**
 * The CashRegister class holds the logic for performing transactions.
 *
 * @param change The change that the CashRegister is holding.
 */
class CashRegister(private val change: Change) {
    /**
     * Performs a transaction for a product/products with a certain price and a given amount.
     *
     * @param price The price of the product(s).
     * @param amountPaid The amount paid by the shopper.
     *
     * @return The change for the transaction.
     *
     * @throws TransactionException If the transaction cannot be performed.
     */
    fun performTransaction(price: Long, amountPaid: Change): Change {
        require(price >= 0L) { "Price must be a positive number." }

        val paidTotal = amountPaid.total
        if (paidTotal < price) {
            throw TransactionException("Insufficient payment. Price: $price, Paid: $paidTotal")
        }

        val changeDue = paidTotal - price
        if (changeDue == 0L) {
            addAll(change, amountPaid)
            return Change.none()
        }

        val tempDrawer = copyOf(change).also { addAll(it,amountPaid)}

        val changeToGive = makeChangeFromInventory(tempDrawer, changeDue) ?: throw TransactionException("Insufficient change.")

        addAll(change, amountPaid)
        removeAll(change,changeToGive)

        return changeToGive
    }

    class TransactionException(message: String, cause: Throwable? = null) : Exception(message, cause)

    private fun copyOf(change: Change): Change {
        val dst = Change()
        for (e in change.getElements()) {
            val c = change.getCount(e)
            if ( c > 0) dst.add(e,c)
        }
        return dst
    }

    private fun addAll(dst: Change, src: Change) {
        for (e in src.getElements()) {
            val c = src.getCount(e)
            if ( c > 0) dst.add(e,c)
        }
    }

    private fun removeAll(dst: Change, src: Change) {
        for (e in src.getElements()) {
            val c = src.getCount(e)
            if (c > 0) dst.remove(e, c)
        }
    }

    private fun makeChangeFromInventory(drawer: Change, amount: Long): Change? {
        var remaining = amount
        val result = Change()

        val denominations = (Bill.entries + Coin.entries).sortedByDescending { it.minorValue }

        for (d in denominations) {
            if (remaining <= 0L) break
            val value = d.minorValue.toLong()
            if (value <= 0L) continue

            val available = drawer.getCount(d)
            if (available <= 0) continue

            val maxNeeded = (remaining / value).toInt()
            val use = minOf(maxNeeded, available)
            if (use > 0) {
                result.add(d, use)
                remaining -= value * use
            }
        }

        return if (remaining == 0L) result else null
    }
}
