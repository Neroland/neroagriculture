package za.co.neroland.neroagriculture.client.compat;

// Minecraft before 1.21.9 only (empty on every later line). 1.21.9 hands screens a MouseButtonEvent; before
// it mouseClicked takes the raw coordinates and button, which the screens wrap in this look-alike so their
// hit tests read the same. The build points net.minecraft.client.input.MouseButtonEvent here on those lines.

//? if <1.21.9 {
/*public record MouseButtonEvent(double x, double y, int button) {
}
*///?}
