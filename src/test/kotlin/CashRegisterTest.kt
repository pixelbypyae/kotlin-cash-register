import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CashRegisterTest {

    private fun drawerOne() : Change = Change().apply {
        Bill.entries.forEach { add(it,10) }
        Coin.entries.forEach { add(it,50) }
    }

    @Test
    fun exactPayment_noChange() {
        val drawer = drawerOne()
        val reg = CashRegister(drawer)

        val paid = Change().apply { add(Bill.TEN_EURO,1) }
        val change = reg.performTransaction(price = 1000, amountPaid = paid)

        assertEquals(0L, change.total, "No change expected")
        assertTrue(drawer.total >= 1000)
    }

    @Test
    fun normalPurchase_returnsCorrectChange() {
        val drawer = drawerOne()
        val reg = CashRegister(drawer)

        val price = 758L
        val paid = Change().apply { add(Bill.TEN_EURO,1) }

        val change = reg.performTransaction(price = price, amountPaid = paid)

        assertEquals(242L, change.total, "Change Due: 2 euro, 42cents")
        assertEquals(1, change.getCount(Coin.TWO_EURO))
        assertEquals(2, change.getCount(Coin.TWENTY_CENT))
        assertEquals(1, change.getCount(Coin.TWO_CENT))
    }

    @Test
    fun insufficientPayment_returnsCorrectChange() {
        val drawer = drawerOne()
        val reg = CashRegister(drawer)

        val paid = Change().apply { add(Coin.FIFTY_CENT,1) }

        val ex = assertThrows(CashRegister.TransactionException::class.java) {
            reg.performTransaction(price = 100L, amountPaid = paid)
        }
        assertTrue(ex.message!!.contains("Insufficient payment"))
    }

    @Test
    fun notEnoughChangeInDrawer_throwsException() {
        val drawer = Change()
        val reg = CashRegister(drawer)

        val paid = Change().apply { add(Bill.FIVE_EURO, 1)}
        val ex = assertThrows(CashRegister.TransactionException::class.java) {
            reg.performTransaction(price = 450, amountPaid = paid)
        }
        assertTrue(ex.message!!.contains("Insufficient change."))
    }

    @Test
    fun largeTransaction_usesLargestBillFirst() {
        val drawer = drawerOne()
        val reg = CashRegister(drawer)

        val price = 500L
        val paid = Change().apply { add(Bill.FIVE_HUNDRED_EURO, 1) }

        val change = reg.performTransaction(price = price, amountPaid = paid)

        assertEquals(49500L, change.total, "Change Due 495.00")
        assertTrue(change.getCount(Bill.TWO_HUNDRED_EURO) >= 2 || change.getCount(Bill.ONE_HUNDRED_EURO) >= 4)
    }

    @Test
    fun `greedy algorithm gives minimum change`() {
        val drawer = Change.max()
        val reg = CashRegister(drawer)

        val price = 758L
        val paid = Change().apply { add(Bill.TEN_EURO, 1) }
        val change = reg.performTransaction(price = price, amountPaid = paid, simulateOnly = true)

        println("change give: $change")

        assertEquals(242L, change.total, "Change due: 2 euro and 42 cents")

        assertEquals(1, change.getCount(Coin.TWO_EURO))
        assertEquals(2, change.getCount(Coin.TWENTY_CENT))
        assertEquals(1, change.getCount(Coin.TWO_CENT))
        assertEquals(0, change.getCount(Coin.ONE_CENT))
    }

}
