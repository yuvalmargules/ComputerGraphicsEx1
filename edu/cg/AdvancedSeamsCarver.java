package edu.cg;

import java.awt.image.BufferedImage;

public class AdvancedSeamsCarver extends BasicSeamsCarver {
	// TODO :  Decide on the fields your AdvancedSeamsCarver should include.
	
	public AdvancedSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super(logger, workingImage, outWidth, outHeight, rgbWeights);
	}
	
	public BufferedImage resizeWithSeamCarving(CarvingScheme carveScheme) {
		if (Math.abs(this.outWidth - this.inWidth) > this.inWidth / 2 || Math.abs(this.outHeight - this.inHeight) > this.inHeight / 2) {
			throw new RuntimeException("Can not apply seam carving: too many seams.");
		}
		logger.log("Scaling image width to " + this.outWidth + " pixels, and height to " + this.outHeight + " pixels.");
		if (this.outWidth <= this.inWidth && this.outHeight <= this.inHeight) {
			return carveImage(carveScheme);
		}
		else if (carveScheme == CarvingScheme.INTERMITTENT){
			throw new IllegalArgumentException("Intermittent carving is not supported in upscaling.");
		}
		else {
			// TODO: Implement the additional seam carving functionalities that AdvancedSeamsCarver offers.
			BufferedImage output = duplicateWorkingImage();
			carveImage(carveScheme);
			if(carveScheme == CarvingScheme.VERTICAL_HORIZONTAL){
				duplicateVerticalSeam(output);
				duplicateHorizontalSeam(output);
			}
			else if(carveScheme == CarvingScheme.HORIZONTAL_VERTICAL){
				duplicateHorizontalSeam(output);
				duplicateVerticalSeam(output);
			}
			return output;
		}
	}


	private void duplicateVerticalSeam(BufferedImage output) {
		int nextRGB, temp;
		Coordinate[] seam;

		for(int j = 0; j < verticalSeamsCount; j++) {
			seam = verticalSeamsRecord[j];

			for (int i = 0; i < seam.length; i++) {
				nextRGB = output.getRGB(seam[i].X, seam[i].Y);
				for (int x = seam[i].X + 1; x < output.getWidth(); x++) {
					temp = output.getRGB(x, seam[i].Y);
					output.setRGB(x, seam[i].Y, nextRGB);
					nextRGB = temp;
				}
			}
		}
	}

	private void duplicateHorizontalSeam(BufferedImage output) {
		int nextRGB, temp;
		Coordinate[] seam;

		for(int j = 0; j < horizontalSeamsCount; j++) {
			seam = horizontalSeamsRecord[j];

			for (int i = 0; i < seam.length; i++) {
				nextRGB = output.getRGB(seam[i].X, seam[i].Y);
				for (int y = seam[i].Y + 1; y < output.getHeight(); y++) {
					temp = output.getRGB(seam[i].X, y);
					output.setRGB(seam[i].X, y, nextRGB);
					nextRGB = temp;
				}
			}
		}
	}


}