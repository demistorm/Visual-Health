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

    public static void stampTexture(NativeImage baseTexture, NativeImage stamp, int posX, int posY) {
        for (int y = 0; y < stamp.getHeight(); y++) {
            for (int x = 0; x < stamp.getWidth(); x++) {
                if (posX + x >= baseTexture.getWidth() || posY + y >= baseTexture.getHeight()) {
                    continue;
                }

                int stampPixel = stamp.getPixel(x, y);
                int stampAlpha = (stampPixel >> 24) & 0xFF;

                if (stampAlpha == 0) {
                    continue;
                }

                int baseX = posX + x;
                int baseY = posY + y;
                int basePixel = baseTexture.getPixel(baseX, baseY);
                int baseAlpha = (basePixel >> 24) & 0xFF;

                if (baseAlpha == 0) {
                    baseTexture.setPixel(baseX, baseY, stampPixel);
                } else {
                    float alphaRatio = stampAlpha / 255.0f;
                    int blendedR = blendChannel((basePixel >> 16) & 0xFF, (stampPixel >> 16) & 0xFF, alphaRatio);
                    int blendedG = blendChannel((basePixel >> 8) & 0xFF, (stampPixel >> 8) & 0xFF, alphaRatio);
                    int blendedB = blendChannel(basePixel & 0xFF, stampPixel & 0xFF, alphaRatio);
                    int blendedA = Math.min(255, baseAlpha + stampAlpha);

                    int blendedPixel = (blendedA << 24) | (blendedR << 16) | (blendedG << 8) | blendedB;
                    baseTexture.setPixel(baseX, baseY, blendedPixel);
                }
            }
        }
    }

    private static int blendChannel(int base, int stamp, float alphaRatio) {
        return (int)(base * (1.0f - alphaRatio) + stamp * alphaRatio);
    }
}
