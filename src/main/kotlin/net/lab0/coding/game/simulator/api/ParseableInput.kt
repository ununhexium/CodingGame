package net.lab0.coding.game.simulator.api

interface ParseableInput<Self> where Self : Input<Self> {
  fun parseStdin(input: String): Input<Self>
}