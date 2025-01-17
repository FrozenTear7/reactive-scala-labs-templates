package EShop.lab3

import EShop.lab2.{Cart, CartActor}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class CartTest
  extends TestKit(ActorSystem("CartTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {
  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  //use GetItems command which was added to make test easier
  it should "add item properly" in {
    val testItem = "POGGERS"

    val cartRef = TestActorRef(CartActor.props())
    cartRef ! CartActor.AddItem(testItem)
    cartRef ! CartActor.GetItems
    expectMsg(List(testItem))
  }

  it should "be empty after adding and removing the same item" in {
    val testItem = "POGGERS"

    val cartRef = TestActorRef(CartActor.props())
    cartRef ! CartActor.AddItem(testItem)
    cartRef ! CartActor.RemoveItem(testItem)
    cartRef ! CartActor.GetItems
    expectMsg(Cart.empty)
  }

  it should "start checkout" in {
    val testItem = "POGGERS"

    val cartRef = TestActorRef(CartActor.props())
    cartRef ! CartActor.AddItem(testItem)
    cartRef ! CartActor.StartCheckout
    expectMsg(_: CartActor.CheckoutStarted)
  }
}
