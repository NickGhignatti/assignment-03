import scalafx.application.JFXApp3
import scalafx.scene.Scene
import model.BoidsModel
import view.BoidsView
import controller.BoidsController

object BoidsApp extends JFXApp3 {
  override def start(): Unit = {
    val model = new BoidsModel()
    val view = new BoidsView()
    val controller = new BoidsController(model, view)

    stage = new JFXApp3.PrimaryStage {
      title = "Boids Simulation"
      scene = new Scene(view.root, 960, 720)
    }
  }
}
