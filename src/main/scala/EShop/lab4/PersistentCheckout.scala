package EShop.lab4

import EShop.lab3.Payment
import akka.actor.{ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object PersistentCheckout {
  def props(cartActor: ActorRef, persistenceId: String) =
    Props(new PersistentCheckout(cartActor, persistenceId))
}

class PersistentCheckout(
  cartActor: ActorRef,
  val persistenceId: String
) extends PersistentActor {
  import EShop.lab2.Checkout._
  private val scheduler                           = context.system.scheduler
  private val log                                 = Logging(context.system, this)
  implicit val executionContext: ExecutionContext = context.system.dispatcher

  val timerDuration: FiniteDuration = 1.seconds
  private def checkoutTimer: Cancellable =
    scheduler.scheduleOnce(timerDuration, self, ExpireCheckout)
  private def paymentTimer: Cancellable =
    scheduler.scheduleOnce(timerDuration, self, ExpirePayment)

  private def updateState(event: Event, maybeTimer: Option[Cancellable] = None): Unit = {
    maybeTimer.foreach(_.cancel())
    event match {
      case CheckoutStarted           => context become selectingDelivery(checkoutTimer)
      case DeliveryMethodSelected(_) => context become selectingPaymentMethod(checkoutTimer)
      case CheckOutClosed            => context become closed
      case CheckoutCancelled         => context become cancelled
      case PaymentStarted(_)         => context become processingPayment(paymentTimer)
    }
  }

  def receiveCommand: Receive = LoggingReceive.withLabel("receiveCommand") {
    case StartCheckout => persist(CheckoutStarted)(event => updateState(event))
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive.withLabel("selectingDelivery") {
    case SelectDeliveryMethod(method) =>
      persist(DeliveryMethodSelected(method))(event => updateState(event, Some(timer)))

    case ExpireCheckout | CancelCheckout =>
      persist(CheckoutCancelled)(event => updateState(event, Some(timer)))
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive.withLabel("selectingPaymentMethod") {
    case SelectPayment(payment) =>
      val paymentActor: ActorRef = context.system.actorOf(Payment.props(payment, sender, self))
      sender ! PaymentStarted(paymentActor)
      persist(PaymentStarted(paymentActor))(event => updateState(event))

    case ExpireCheckout | CancelCheckout => persist(CheckoutCancelled)(event => updateState(event, Some(timer)))
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive.withLabel("processingPayment") {
    case ReceivePayment =>
      cartActor ! CheckOutClosed
      persist(CheckOutClosed)(event => updateState(event, Some(timer)))

    case ExpireCheckout | CancelCheckout => persist(CheckoutCancelled)(event => updateState(event, Some(timer)))
  }

  def cancelled: Receive = LoggingReceive.withLabel("cancelled") {
    case _ =>
  }

  def closed: Receive = LoggingReceive.withLabel("closed") {
    case _ =>
  }

  override def receiveRecover: Receive = {
    case event: Event     => updateState(event)
    case _: SnapshotOffer => log.error("Received unhandled snapshot offer")
  }
}
