package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.Random;

public final class WoundTextureUtils {

    private WoundTextureUtils() {
    }

    public static int[] shuffleGrid(int textureWidth, int textureHeight, Random random) {
        int gridCols = textureWidth / 8;
        int gridRows = textureHeight / 8;
        int totalCells = gridRows * gridCols;

        int[] cells = new int[totalCells];
        for (int i = 0; i < totalCells; i++) {
            cells[i] = i;
        }

        for (int i = totalCells - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = cells[i];
            cells[i] = cells[j];
            cells[j] = temp;
        }

        return cells;
    }

    public static int[] getFuzzyGridPosition(int textureWidth, int textureHeight,
                                           int woundWidth, int woundHeight,
                                           int[] shuffledCells, int woundIndex,
                                           Random random) {
        int gridCols = textureWidth / 8;
        int gridRows = textureHeight / 8;
        int cellWidth = 8;
        int cellHeight = 8;

        int totalCells = gridRows * gridCols;
        int cellIndex = shuffledCells[woundIndex % totalCells];
        int cellRow = cellIndex / gridCols;
        int cellCol = cellIndex % gridCols;

        int centerX = cellCol * cellWidth + cellWidth / 2;
        int centerY = cellRow * cellHeight + cellHeight / 2;

        int offsetX = random.nextInt(17) - 8;
        int offsetY = random.nextInt(17) - 8;

        int x = centerX + offsetX - woundWidth / 2;
        int y = centerY + offsetY - woundHeight / 2;

        x = Math.max(0, Math.min(textureWidth - woundWidth, x));
        y = Math.max(0, Math.min(textureHeight - woundHeight, y));

        return new int[]{x, y};
    }

    public static void stampTexture(NativeImage canvas, NativeImage woundAsset, int tint,
                                    int posX, int posY, float opacity) {
        int tintR = tint & 0xFF;
        int tintG = (tint >> 8) & 0xFF;
        int tintB = (tint >> 16) & 0xFF;

        for (int y = 0; y < woundAsset.getHeight(); y++) {
            for (int x = 0; x < woundAsset.getWidth(); x++) {
                if (posX + x >= canvas.getWidth() || posY + y >= canvas.getHeight()) {
                    continue;
                }

                int stampPixel = woundAsset.getPixelRGBA(x, y);
                int stampAlpha = (stampPixel >> 24) & 0xFF;

                if (stampAlpha == 0) {
                    continue;
                }

                int r = stampPixel & 0xFF;
                int g = (stampPixel >> 8) & 0xFF;
                int b = (stampPixel >> 16) & 0xFF;

                int tintedR = (r * tintR) / 255;
                int tintedG = (g * tintG) / 255;
                int tintedB = (b * tintB) / 255;

                int effectiveAlpha = Math.min(255, (int)(stampAlpha * opacity));
                if (effectiveAlpha == 0) {
                    continue;
                }

                int baseX = posX + x;
                int baseY = posY + y;
                int basePixel = canvas.getPixelRGBA(baseX, baseY);
                int baseAlpha = (basePixel >> 24) & 0xFF;

                if (baseAlpha == 0) {
                    int adjustedPixel = (effectiveAlpha << 24) | (tintedB << 16) | (tintedG << 8) | tintedR;
                    canvas.setPixelRGBA(baseX, baseY, adjustedPixel);
                } else {
                    int baseR = basePixel & 0xFF;
                    int baseG = (basePixel >> 8) & 0xFF;
                    int baseB = (basePixel >> 16) & 0xFF;

                    float alphaRatio = effectiveAlpha / 255.0f;
                    int blendedR = blendChannel(baseR, tintedR, alphaRatio);
                    int blendedG = blendChannel(baseG, tintedG, alphaRatio);
                    int blendedB = blendChannel(baseB, tintedB, alphaRatio);
                    int blendedA = Math.min(255, baseAlpha + effectiveAlpha);

                    int blendedPixel = (blendedA << 24) | (blendedB << 16) | (blendedG << 8) | blendedR;
                    canvas.setPixelRGBA(baseX, baseY, blendedPixel);
                }
            }
        }
    }

    public static boolean isWoundVisible(boolean[] cellGrid, int gridCols,
                                         int posX, int posY, int woundWidth, int woundHeight) {
        int cellX1 = posX / 8;
        int cellY1 = posY / 8;
        int cellX2 = (posX + woundWidth - 1) / 8;
        int cellY2 = (posY + woundHeight - 1) / 8;

        for (int cy = cellY1; cy <= cellY2; cy++) {
            for (int cx = cellX1; cx <= cellX2; cx++) {
                if (cellGrid[cy * gridCols + cx]) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int blendChannel(int base, int stamp, float alphaRatio) {
        return (int)(base * (1.0f - alphaRatio) + stamp * alphaRatio);
    }
}
