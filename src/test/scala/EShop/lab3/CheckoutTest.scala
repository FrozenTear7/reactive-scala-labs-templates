package EShop.lab3

import EShop.lab2.CartActor.CloseCheckout
import EShop.lab2.Checkout
import EShop.lab2.Checkout.PaymentStarted
import EShop.lab3.Payment.DoPayment
import akka.actor.ActorSystem
import akka.testkit._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

class CheckoutTest
  extends TestKit(ActorSystem("CheckoutTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {
  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  it should "Send close confirmation to cart" in {
    val deliveryMethod = "AMAZON_PRIME"
    val paymentType    = "PAYPAL_OMEGALUL"

    val checkout = TestActorRef(new Checkout(self))

    checkout ! Checkout.StartCheckout
    checkout ! Checkout.SelectDeliveryMethod(deliveryMethod)
    checkout ! Checkout.SelectPayment(paymentType)

    val payment = expectMsgPF() {
      case PaymentStarted(paymentService) => paymentService
    }

    payment ! DoPayment

    expectMsg(CloseCheckout)
  }
}
