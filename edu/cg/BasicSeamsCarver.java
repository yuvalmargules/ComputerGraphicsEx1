package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;


public class BasicSeamsCarver extends ImageProcessor {

	// An enum describing the carving scheme used by the seams carver.
	// VERTICAL_HORIZONTAL means vertical seams are removed first.
	// HORIZONTAL_VERTICAL means horizontal seams are removed first.
	// INTERMITTENT means seams are removed intermittently : vertical, horizontal, vertical, horizontal etc.
	public static enum CarvingScheme {
		VERTICAL_HORIZONTAL("Vertical seams first"),
		HORIZONTAL_VERTICAL("Horizontal seams first"),
		INTERMITTENT("Intermittent carving");

		public final String description;

		private CarvingScheme(String description) {
			this.description = description;
		}
	}

	// A simple coordinate class which assists the implementation.
	protected class Coordinate{
		public int X;
		public int Y;
		public Coordinate(int X, int Y) {
			this.X = X;
			this.Y = Y;
		}
	}

	// TODO :  Decide on the fields your BasicSeamsCarver should include. Refer to the recitation and homework 
			// instructions PDF to make an educated decision.
	public double[][] pixelEnergyMatrix;
	public Coordinate[][] pixelOrigin;
	public Coordinate[][] verticalSeamsRecord;
	public Coordinate[][] horizontalSeamsRecord;
	public int[][] minimumCoordinates;
	public int[][] greyImgValues;
	int carvedHeight;
	int carvedWidth;
	int verticalSeamsCount = 0;
	int horizontalSeamsCount = 0;

	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		carvedWidth = inWidth;
		carvedHeight = inHeight;
		verticalSeamsRecord = new Coordinate[ Math.abs(this.outWidth - this.inWidth)][];
		horizontalSeamsRecord = new Coordinate[ Math.abs(this.outHeight - this.inHeight)][];

		initCarvedImg();
		initGreyImageValues();
		initPixelOriginCoordinates();

	}
	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);

		if (carvingScheme == CarvingScheme.VERTICAL_HORIZONTAL)
		{
			carveVerticalSeams(numberOfVerticalSeamsToCarve);
			carveHorizontalSeams(numberOfHorizontalSeamsToCarve);
		}
		else if (carvingScheme == CarvingScheme.HORIZONTAL_VERTICAL){
			carveHorizontalSeams(numberOfHorizontalSeamsToCarve);
			carveVerticalSeams(numberOfVerticalSeamsToCarve);
		}
		else if (carvingScheme == CarvingScheme.INTERMITTENT){
			int totalSeamsToCarve = numberOfHorizontalSeamsToCarve + numberOfVerticalSeamsToCarve;
			int verticalSeamsCarved = 0;
			int horizontalSeamsCarved = 0;
			while (verticalSeamsCarved + horizontalSeamsCarved < totalSeamsToCarve){
				if (verticalSeamsCarved < numberOfVerticalSeamsToCarve){
					carveVerticalSeams(1);
					verticalSeamsCarved++;
				}
				if(horizontalSeamsCarved < numberOfHorizontalSeamsToCarve){
					carveHorizontalSeams(1);
					horizontalSeamsCarved++;
				}
			}
		}
		return createCarvedImg();
	}

	public BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);

		if(showVerticalSeams){
			carveVerticalSeams(numberOfVerticalSeamsToCarve);
			return getSeamPaintedImg(seamColorRGB, true);
			}
		else {
			carveHorizontalSeams(numberOfHorizontalSeamsToCarve);
			return getSeamPaintedImg(seamColorRGB, false);
		}
	}

	private void initPixelEnergyMatrix() {
		for (int i = 0; i < carvedWidth; i++) {
			for (int j = 0; j < carvedHeight; j++) {
				pixelEnergyMatrix[j][i] = pixelEnergy(j, i);
			}
		}
	}

	private void initCarvedImg() {
		pixelEnergyMatrix = new double[carvedHeight][carvedWidth];
		minimumCoordinates = new int[carvedHeight][carvedWidth];
	}

	private void initPixelOriginCoordinates(){
		pixelOrigin = new Coordinate[inHeight][inWidth];
		for( int i = 0; i < inWidth; i++) {
			for (int j = 0; j < inHeight; j++) {
				pixelOrigin[j][i] = new Coordinate(i, j);
			}
		}
	}

	private void initGreyImageValues() {
		greyImgValues = new int[inHeight][inWidth];
		BufferedImage greyImg = greyscale();
		for (int i = 0; i < inWidth; i++) {
			for (int j = 0; j < inHeight; j++) {
				greyImgValues[j][i] = new Color(greyImg.getRGB(i, j)).getGreen();
			}
		}
	}

	private BufferedImage getSeamPaintedImg(int seamColorRGB, Boolean isVertical) {
		int count = horizontalSeamsCount;
		Coordinate[][] record = horizontalSeamsRecord;
		if( isVertical){
			count = verticalSeamsCount;
			record = verticalSeamsRecord;
		}

		BufferedImage img = duplicateWorkingImage();
		for( int i = 0; i < count; i++){
			paintSeam(img, record[i], seamColorRGB);
		}

		return img;
	}

	private void paintSeam(BufferedImage img, Coordinate[] coordinates, int seamColorRGB) {
		for(int i = 0 ; i < coordinates.length; i++){
			img.setRGB(coordinates[i].X, coordinates[i].Y, seamColorRGB);
		}
	}



	private double pixelEnergy(int y, int x) {
		int i = x + 1, j = y + 1;;
		if (x == carvedWidth - 1) {
			i = x - 1;
		}
		if (y == carvedHeight - 1) {
			j = y - 1;
		}
		double dy = Math.pow(Math.abs(greyImgValues[y][i] - greyImgValues[y][x]), 2);
		double dx = Math.pow(Math.abs(greyImgValues[j][x] - greyImgValues[y][x]), 2);

		return Math.sqrt(dx + dy);
	}



	private void carveHorizontalSeams(int numberOfHorizontalSeamsToCarve) {
		for(int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
			initPixelEnergyMatrix();
			findHorizontalMinCost();
			Coordinate[] seam = findHorizontalSeam();
			horizontalSeamsRecord[horizontalSeamsCount++] = seam.clone();
			carveHorizontalSeam(seam);
			carvedHeight--;
		}
	}

	private void carveVerticalSeams(int numberOfVerticalSeamsToCarve) {
		for(int i = 0; i < numberOfVerticalSeamsToCarve; i++) {
			initPixelEnergyMatrix();
			findVerticalMinCost();
			Coordinate[] seam = findVerticalSeam();
			verticalSeamsRecord[verticalSeamsCount++] = seam.clone();
			carveVerticalSeam(seam);
			carvedWidth--;
		}
	}

	private BufferedImage createCarvedImg() {
		BufferedImage img = newEmptyOutputSizedImage();

		for(int y = 0; y < carvedHeight; y++) {
			for(int x = 0; x < carvedWidth; x++) {
				Coordinate origin = pixelOrigin[y][x];
				img.setRGB(x, y, workingImage.getRGB(origin.X, origin.Y));
			}
		}

		return img;
	}

	private void carveVerticalSeam(Coordinate[] seam) {
		for(int i = 0; i < seam.length; i++){
			for(int x = seam[i].X + 1; x < carvedWidth; x++){
				pixelOrigin[seam[i].Y][x - 1] = pixelOrigin[seam[i].Y][x];
				greyImgValues[seam[i].Y][x - 1] = greyImgValues[seam[i].Y][x];
			}
		}
	}

	private void carveHorizontalSeam(Coordinate[] seam) {
		for(int i = 0; i < seam.length; i++){
			for(int y = seam[i].Y + 1; y < carvedHeight; y++){
				pixelOrigin[y - 1][seam[i].X] = pixelOrigin[y][seam[i].X];
				greyImgValues[y - 1][seam[i].X] = greyImgValues[y][seam[i].X];
			}
		}
	}

	private Coordinate[] findVerticalSeam() {
		Coordinate[] seam = new Coordinate[carvedHeight];
		int min = 0;
		for(int y = 0; y < carvedWidth; y++){
			if(pixelEnergyMatrix[carvedHeight - 1][y] < pixelEnergyMatrix[carvedHeight - 1][min])
				min = y;
		}

		for(int y = carvedHeight - 1; y >= 0; y--){
			seam[y] = new Coordinate(min, y);
			min = minimumCoordinates[y][min];
		}

		return seam;
	}
	private Coordinate[] findHorizontalSeam() {
		Coordinate[] seam = new Coordinate[carvedWidth];
		int min = 0;

		for(int x = 0; x < carvedHeight; x++){
			if(pixelEnergyMatrix[x][carvedWidth - 1] < pixelEnergyMatrix[min][carvedWidth - 1])
				min = x;
		}

		for(int x = carvedWidth - 1; x >= 0; x--){
			seam[x] = new Coordinate(x, min);
			min = minimumCoordinates[min][x];
		}

		return seam;
	}

	private void findVerticalMinCost() {
		for(int y = 0; y < carvedHeight; y++) {
			for(int x = 0; x < carvedWidth; x++) {
				findVerticalMinCostPerPixel(y, x);
			}
		}
	}

	private void findHorizontalMinCost() {
		for(int y = 0; y < carvedWidth; y++) {
			for(int x = 0; x < carvedHeight; x++) {
				findHorizontalMinCostPerPixel(x, y);
			}
		}
	}

	private void findHorizontalMinCostPerPixel(int y, int x) {
		double min = 0;
		double Cu = 0;
		double Mu = Integer.MAX_VALUE;
		double Ch = 0;
		double Mh = Integer.MAX_VALUE;
		double Cd = 0;
		double Md = Integer.MAX_VALUE;

		if (x > 0) {
			if (y > 0 && y != carvedHeight - 1) {
				Ch = Math.abs(greyImgValues[y - 1][x] - greyImgValues[y + 1][x]);
				Mh = pixelEnergyMatrix[y][x - 1];
			}
			if (y > 0) {
				Cu = Math.abs(greyImgValues[y - 1][x] - greyImgValues[y][x - 1]) + Ch;
				Mu = pixelEnergyMatrix[y - 1][x - 1];
			}
			if (y != carvedHeight - 1){
				Cd = Math.abs(greyImgValues[y + 1][x] - greyImgValues[y][x - 1]) + Ch;
				Md = pixelEnergyMatrix[y + 1][x - 1];
			}
			min = Math.min(Math.min(Mu + Cu, Mh + Ch), Md + Cd);
		}

		int minY = y;
		if (min == Mu + Cu && y > 0)
			minY -= 1;
		else if (min == Md + Cd && y != carvedHeight - 1)
			minY += 1;

		minimumCoordinates[y][x] = minY;
		pixelEnergyMatrix[y][x] += min;
	}

	private void findVerticalMinCostPerPixel(int y, int x) {
		double min = 0;
		double Cl = 0;
		double Ml = Integer.MAX_VALUE;
		double Cv = 0;
		double Mv = Integer.MAX_VALUE;
		double Cr = 0;
		double Mr = Integer.MAX_VALUE;

		if (y > 0) {
			if (x > 0 && x != carvedWidth - 1) {
				Cv = Math.abs(greyImgValues[y][x - 1] - greyImgValues[y][x + 1]);
				Mv = pixelEnergyMatrix[y - 1][x];
			}
			if (x > 0) {
				Cl = Math.abs(greyImgValues[y][x - 1] - greyImgValues[y - 1][x]) + Cv;
				Ml = pixelEnergyMatrix[y - 1][x - 1];
			}
			if (x != carvedWidth - 1){
				Cr = Math.abs(greyImgValues[y][x + 1] - greyImgValues[y - 1][x]) + Cv;
				Mr = pixelEnergyMatrix[y - 1][x + 1];
			}
			min = Math.min(Math.min(Ml + Cl, Mv + Cv), Mr + Cr);
		}

		int minX = x;
		if (min == Ml + Cl && x > 0)
			minX -= 1;
		else if (min == Mr + Cr && x != carvedWidth - 1)
			minX += 1;

		minimumCoordinates[y][x] = minX;
		pixelEnergyMatrix[y][x] += min;
	}



}
