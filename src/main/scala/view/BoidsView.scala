package view

import scalafx.scene.control._
import scalafx.scene.canvas.Canvas
import scalafx.scene.layout._
import scalafx.scene.text.Text
import scalafx.geometry.Insets

class BoidsView {
  val startButton = new Button("START")
  val pauseButton = new Button("RESUME/STOP")
  val resetButton = new Button("RESET")
  val boidInput: TextField = new TextField {
    promptText = "0"
    prefWidth = 60
  }

  private val topBar = new HBox(5, startButton, pauseButton, resetButton, boidInput) {
    padding = Insets(10)
  }

  val canvas = new Canvas(800, 800)

  val separationSlider: Slider = new Slider(0, 2, 1) {
    majorTickUnit = 1
    showTickMarks = true
  }
  val alignmentSlider: Slider = new Slider(0, 2, 1) {
    majorTickUnit = 1
    showTickMarks = true
  }
  val cohesionSlider: Slider = new Slider(0, 2, 1) {
    majorTickUnit = 1
    showTickMarks = true
  }

  private val bottomBar = new HBox(10,
    new Label("Separation"), separationSlider,
    new Label("Alignment"), alignmentSlider,
    new Label("Cohesion"), cohesionSlider
  ) {
    padding = Insets(10)
  }

  val infoText = new Text("Num. Boids: 0\nFramerate: 25")
  private val leftBar = new VBox(5, infoText) {
    padding = Insets(10)
  }

  val root: BorderPane = new BorderPane {
    top = topBar
    center = canvas
    bottom = bottomBar
    left = leftBar
  }

  def getWinProps: (Double, Double, Double) = {
    canvas.localToScene(0, 0) match {
      case p => (p.getX, p.getY, 800.0)
    }
  }
}