package net.lab0.coding.game.simulator.codersstrikesback

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import net.lab0.coding.game.simulator.ui.Context
import net.lab0.coding.game.simulator.ui.ResizableCanvas

data class RacerDrawContext(
    val ratio: Double,
    val canvas: Canvas,
    val gc: GraphicsContext
): Context<RacerDrawContext>
