# General Notes

## Render Pipeline
1. Mix into start of Minecraft frame; call `glViewport` with the values of the main viewport as dictated by the UI.

    - Framebuffer must also be resized to fit the viewport.

2. Let Minecraft render normally. Because of the `glViewport` call, it will only render to part of the screen.

3. After the screen blits, render the editor UI as an "overlay" to the main window.

    - Individual panels may call back into Minecraft's rendering code for specific items (entity rendering, etc).