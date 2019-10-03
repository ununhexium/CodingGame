package net.lab0.coding.game.simulator.ui

import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color

class ResizableCanvas : Canvas() {
  init {
    // Redraw canvas when size changes.
    widthProperty().addListener { _ -> draw() }
    heightProperty().addListener { _ -> draw() }
  }

  private fun draw() {
    println("Resize")
    val gc = graphicsContext2D
    gc.clearRect(0.0, 0.0, width, height)

    gc.stroke = Color.RED
    gc.strokeLine(0.0, 0.0, width, height)
    gc.strokeLine(0.0, height, width, 0.0)
  }

  override fun isResizable() = true

  override fun prefWidth(height: Double) = width

  override fun prefHeight(width: Double) = height

  override fun minHeight(width: Double) = 128.0

  override fun minWidth(height: Double) = 128.0

  override fun maxHeight(width: Double) = Double.POSITIVE_INFINITY

  override fun maxWidth(height: Double) = Double.POSITIVE_INFINITY

  override fun resize(width: Double, height: Double) {
    this.width = width;
    this.height = height
  }
}