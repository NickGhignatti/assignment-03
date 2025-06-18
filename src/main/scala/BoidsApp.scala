import scalafx.application.JFXApp3
import scalafx.scene.Scene
import controller.BoidsController
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import controller.BoidsController.{PauseResumeSimulation, ResetSimulation, SetBoidsCount, StartSimulation}
import view.BoidsView
import model.BoidsModel
import view.BoidsViewActor

object BoidsApp extends JFXApp3 {
  override def start(): Unit = {
    val view = new BoidsView()
    
    val system = ActorSystem(Behaviors.empty, "BoidsSystem")
    val model = system.systemActorOf(BoidsModel(), "BoidsModelActor")
    val viewActor = system.systemActorOf(BoidsViewActor(view), "BoidsViewActor")

    val controller = ActorSystem(BoidsController(model, viewActor), "BoidsSystem")

    stage = new JFXApp3.PrimaryStage {
      title = "Boids Simulation"
      scene = new Scene {
        root = view.root
      }
    }

    // Connect buttons to controller
    view.startButton.onAction = _ => {
      println(s"In app starting simulation with ${view.boidInput.text.value} boids")
      controller ! StartSimulation(view.boidInput.text.value.toIntOption.getOrElse(0))
    }
    view.pauseButton.onAction = _ => controller ! PauseResumeSimulation()
    view.resetButton.onAction = _ => controller ! ResetSimulation()
    view.boidInput.onAction = _ => {
      val count = view.boidInput.text.value.toIntOption.getOrElse(0)
      controller ! SetBoidsCount(count)
    }
  }
}