import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import view.BoidsView
import model.BoidsModel
import view.BoidsViewActor

object BoidsApp extends JFXApp3 {
  override def start(): Unit = {
    val view = new BoidsView()

    val system = ActorSystem(Behaviors.empty, "BoidsSystem")
    val model = system.systemActorOf(BoidsModel(), "BoidsModelActor")
    val modelView = ActorSystem(BoidsViewActor(view, model), "BoidsViewActor")

    stage = new JFXApp3.PrimaryStage {
      title = "Boids Simulation"
      scene = new Scene {
        root = view.root
      }
      onCloseRequest = _ => System.exit(0)

    }
  }
}