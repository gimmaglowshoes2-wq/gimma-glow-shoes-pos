package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.StoreConfig
import com.example.printer.ThermalReceiptHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun testAppNameResource() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GlowShoes POS", appName)
  }

  @Test
  fun testRoomDatabaseEntities() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)

    // Test MenuItem entity & DAO
    val menuItem = MenuItem(
      name = "Deep Clean Sneaker",
      price = 50000.0,
      estimatedDays = 2,
      category = "Deep Clean"
    )
    val menuId = db.menuItemDao().insertMenuItem(menuItem)
    val retrievedMenu = db.menuItemDao().getMenuItemById(menuId)
    assertNotNull(retrievedMenu)
    assertEquals("Deep Clean Sneaker", retrievedMenu?.name)

    // Test Customer entity & DAO
    val customer = Customer(
      name = "Andi Wijaya",
      phone = "081234567890",
      totalPoints = 100,
      tier = "Silver"
    )
    val customerId = db.customerDao().insertCustomer(customer)
    val retrievedCustomer = db.customerDao().getCustomerById(customerId)
    assertNotNull(retrievedCustomer)
    assertEquals("Andi Wijaya", retrievedCustomer?.name)

    // Test Order entity & DAO
    val order = Order(
      invoiceNumber = "GS-260927-123456",
      customerId = customerId,
      customerName = "Andi Wijaya",
      customerPhone = "081234567890",
      subtotal = 50000.0,
      finalTotal = 50000.0,
      paymentMethod = "Tunai"
    )
    val orderId = db.orderDao().insertOrder(order)
    val retrievedOrder = db.orderDao().getOrderById(orderId)
    assertNotNull(retrievedOrder)
    assertEquals("GS-260927-123456", retrievedOrder?.invoiceNumber)
  }

  @Test
  fun testThermalReceiptText() {
    val config = StoreConfig(
      storeName = "GlowShoes Laundry",
      storeAddress = "Jl. Merdeka 45",
      storePhone = "081234567890"
    )
    val order = Order(
      invoiceNumber = "GS-260927-001",
      customerName = "Budi",
      customerPhone = "08123456789",
      subtotal = 50000.0,
      finalTotal = 50000.0,
      paymentMethod = "Tunai"
    )
    val items = listOf(
      OrderItem(
        serviceName = "Deep Clean",
        shoeBrandModel = "Nike Jordan",
        shoeColor = "White",
        price = 50000.0,
        quantity = 1
      )
    )
    val receipt = ThermalReceiptHelper.buildTextReceipt(order, items, config)
    assertTrue(receipt.contains("GlowShoes Laundry"))
    assertTrue(receipt.contains("GS-260927-001"))
    assertTrue(receipt.contains("Deep Clean"))
    assertTrue(receipt.contains("Nike Jordan"))
  }
}
