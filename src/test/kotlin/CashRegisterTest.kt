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
    fun `throws exception when price is negative`() {
        // Given
        val drawer = drawerOne()
        val reg = CashRegister(drawer)

        val price = -100L
        val paid = Change().apply { add(Bill.TEN_EURO,1) }

        // When / Then
        val ex = assertThrows(IllegalArgumentException::class.java) {
            reg.performTransaction(price = price, amountPaid = paid)
        }
        assertTrue(ex.message!!.contains("Price must be a positive number."))
    }

    @Test
    fun `exact payment simulateOnly=true does not mutate drawer`() {
        val drawer = drawerOne()
        val beforeTotal = drawer.total
        val reg = CashRegister(drawer)

        val paid = Change().apply { add(Bill.TEN_EURO,1) }
        val change = reg.performTransaction(price = 1000, amountPaid = paid, simulateOnly = true)

        assertEquals(0L, change.total, "No change expected")
        assertEquals(beforeTotal, drawer.total, "Drawer should not be mutated")

        val change2 = reg.performTransaction(price = 1000, amountPaid = paid)

        assertEquals(0L, change2.total, "No change expected")
        assertEquals(beforeTotal + 1000, drawer.total, "Drawer should be mutated")
    }

    @Test
    fun `zero price path returns all paid as change drawer unchanged in both modes`() {
        val drawer = Change().apply {
            add(Coin.TWENTY_CENT, 10)
            add(Coin.TEN_CENT,10)
        }

        val reg = CashRegister(drawer)
        val before = drawer.total

        val paid = Change().apply { add(Coin.TWENTY_CENT, 3) }

        val c1 = reg.performTransaction(price = 0, amountPaid = paid, simulateOnly = true)
        assertEquals(60L,c1.total)

        // drawer unchanged
        assertEquals(before, drawer.total)

        val c2 = reg.performTransaction(price = 0, amountPaid = paid)
        assertEquals(60L,c2.total)
        assertEquals(before, drawer.total)
    }

    @Test
    fun `drawer mutation removes given change`() {
        val drawer = Change().apply {
            add(Bill.TEN_EURO, 1)
            add(Coin.TWO_EURO,1)
            add(Coin.TWENTY_CENT, 2)
            add(Coin.TWO_CENT, 1)
        }
        val reg = CashRegister(drawer)

        val paid = Change().apply { add(Bill.TEN_EURO,1) }
        val price = 758L
        val before = drawer.total
        val change = reg.performTransaction(price = price, amountPaid = paid)

        assertEquals(242L,change.total)

        assertEquals(before + price, drawer.total)

        assertEquals(0, drawer.getCount(Coin.TWENTY_CENT))
        assertEquals(0, drawer.getCount(Coin.TWO_CENT))
    }

    @Test
    fun `exactPayment noChange`() {
        val drawer = drawerOne()
        val reg = CashRegister(drawer)

        val paid = Change().apply { add(Bill.TEN_EURO,1) }
        val change = reg.performTransaction(price = 1000, amountPaid = paid)

        assertEquals(0L, change.total, "No change expected")
        assertTrue(drawer.total >= 1000)
    }

    @Test
    fun `normalPurchase returnsCorrectChange`() {
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
    fun `insufficientPayment returns CorrectChange`() {
        val drawer = drawerOne()
        val reg = CashRegister(drawer)

        val paid = Change().apply { add(Coin.FIFTY_CENT,1) }

        val ex = assertThrows(CashRegister.TransactionException::class.java) {
            reg.performTransaction(price = 100L, amountPaid = paid)
        }
        assertTrue(ex.message!!.contains("Insufficient payment"))
    }

    @Test
    fun `notEnoughChangeInDrawer throwsException`() {
        val drawer = Change()
        val reg = CashRegister(drawer)

        val paid = Change().apply { add(Bill.FIVE_EURO, 1)}
        val ex = assertThrows(CashRegister.TransactionException::class.java) {
            reg.performTransaction(price = 450, amountPaid = paid)
        }
        assertTrue(ex.message!!.contains("Insufficient change."))
    }

    @Test
    fun `largeTransaction usesLargestBillFirst`() {
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
