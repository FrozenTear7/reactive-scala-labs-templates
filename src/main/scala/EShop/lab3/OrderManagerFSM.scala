package EShop.lab3

import EShop.lab2._
import EShop.lab3.OrderManager._
import EShop.lab3.Payment.DoPayment
import akka.actor.FSM

class OrderManagerFSM extends FSM[State, Data] {
  startWith(Uninitialized, Empty)

  when(Uninitialized) {
    case Event(event: AddItem, _) =>
      val cartActor = context.system.actorOf(CartActor.props())
      cartActor ! CartActor.AddItem(event.id)
      sender ! Done
      goto(Open) using CartData(cartActor)
  }

  when(Open) {
    case Event(event: AddItem, cartData: CartData) =>
      cartData.cartRef ! CartActor.AddItem(event.id)
      sender ! Done
      stay

    case Event(event: RemoveItem, cartData: CartData) =>
      cartData.cartRef ! CartActor.RemoveItem(event.id)
      sender ! Done
      stay

    case Event(Buy, cartData: CartData) =>
      cartData.cartRef ! CartActor.StartCheckout
      goto(InCheckout) using CartDataWithSender(cartData.cartRef, sender)
  }

  when(InCheckout) {
    case Event(event: CartActor.CheckoutStarted, cartData: CartDataWithSender) =>
      cartData.cartRef ! Done
      stay using InCheckoutData(event.checkoutRef)

    case Event(event: SelectDeliveryAndPaymentMethod, inCheckoutData: InCheckoutData) =>
      inCheckoutData.checkoutRef ! Checkout.SelectDeliveryMethod(event.delivery)
      inCheckoutData.checkoutRef ! Checkout.SelectPayment(event.payment)
      goto(InPayment) using InCheckoutDataWithSender(inCheckoutData.checkoutRef, sender)
  }

  when(InPayment) {
    case Event(event: Checkout.PaymentStarted, inCheckoutData: InCheckoutDataWithSender) =>
      inCheckoutData.checkoutRef ! Done
      stay using InPaymentData(event.payment)

    case Event(Pay, inPaymentData: InPaymentData) =>
      inPaymentData.paymentRef ! DoPayment
      stay using InPaymentDataWithSender(inPaymentData.paymentRef, sender)

    case Event(Payment.PaymentConfirmed, inPaymentData: InPaymentDataWithSender) =>
      inPaymentData.paymentRef ! Done
      goto(Finished)
  }

  when(Finished) {
    case _ =>
      sender ! "order manager finished job"
      stay
  }
}
