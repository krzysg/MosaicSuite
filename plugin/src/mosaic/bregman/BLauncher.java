package mosaic.bregman;

/*
 * Colocalization analysis class
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;
import ij.plugin.Resizer;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import org.supercsv.cellprocessor.ift.CellProcessor;

import mosaic.bregman.Tools;
import mosaic.bregman.FindConnectedRegions.Region;
import mosaic.core.utils.MosaicUtils;
import mosaic.bregman.output.CSVOutput;
import mosaic.core.ipc.InterPluginCSV;

public class BLauncher 
{
	public  int hcount=0;
	public  String headlesscurrent;
	PrintWriter out3;
	String wpath;
	ImagePlus aImp;
	Tools Tools;
	RScript script;

	String choice1[] = {
			"Automatic", "Low layer", "Medium layer", "High layer"};
	String choice2[] = {
			"Poisson", "Gauss"};
	
	double colocsegAB = 0;
	double colocsegBA = 0;
	
	public BLauncher(String path)
	{
		
		wpath=path;

				
		boolean processdirectory =(new File(wpath)).isDirectory();
		if(processdirectory)
		{
			//IJ.log("Processing directory");
			Headless_directory();
		}
		else
		{
			Headless_file();
			
			if (Analysis.p.save_images)
			{
				//IJ.run(over,"RGB Color", "");
				
				for (int i = 0 ; i < out_over.length ; i++)
				{
					String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_outline_overlay_c" + (i+1) + ".zip";
					if (out_over != null)
						IJ.saveAs(out_over[i], "ZIP", savepath);
				}
				
				// Write a file info output
				
				PrintWriter out = null;
				File fl = new File(path);
				try
				{out = writeImageDataCsv(out, fl.getAbsolutePath(), fl.getName(), 0);
				out.close();} 
				catch (FileNotFoundException e) 
				{e.printStackTrace();}
			}
		}
	}

	public BLauncher(ImagePlus aImp_)
	{		
		wpath = null;
		aImp = aImp_;
		
		PrintWriter out = null;
		
		// Check if we have more than one frame
		
		for (int f = 1 ; f <= aImp.getNFrames(); f++)
		{
			aImp.setPosition(aImp.getChannel(),aImp.getSlice(),f);
			Headless_file();
			
			// Write a file info output
			
			if (Analysis.p.save_images)
			{
				try
				{out = writeImageDataCsv(out, MosaicUtils.ValidFolderFromImage(aImp), aImp.getTitle(), f-1);} 
				catch (FileNotFoundException e) 
				{e.printStackTrace();}
			}
		}
		
		out.close();
		
		// Save images
		
		if (Analysis.p.save_images)
		{
			//IJ.run(over,"RGB Color", "");
			
			for (int i = 0 ; i < out_over.length ; i++)
			{
				String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_outline_overlay_c" + (i+1) + ".zip";
				if (out_over != null)
					IJ.saveAs(out_over[i], "ZIP", savepath);
			}
			
			for (int i = 0 ; i < out_disp.length ; i++)
			{
				String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_intensities" + "_c"+(i+1)+".zip";
				IJ.saveAs(out_disp[i], "ZIP", savepath);
			}
		}
	}
	
	/**
	 * 
	 * Write the CSV ImageData file information
	 * 
	 * @param path Path of the file path
	 * @return true if success, false otherwise
	 * @throws FileNotFoundException 
	 */
	
	public PrintWriter writeImageDataCsv(PrintWriter out, String path, String filename, int hcount) throws FileNotFoundException
	{
		if (out == null)
		{
			// Remove extension from filename
			
			String[] fl = filename.split(".");
			
			out  = new PrintWriter(path + File.separator + fl[0] + "_ImagesData"+ ".csv");
		}
			
		// if two channel
		
		if (hcount == 0)
		{
			// write the header
			
			if (Analysis.p.nchannels == 2)
			{
				out.print("File"+ ";" +"Image ID" + ";"+ "Objects ch1" + ";" + "Mean size in ch1"  +";" + "Mean surface in ch1"  +";"+ "Mean length in ch1"  +";"  
						+ "Objects ch2"+";" + "Mean size in ch2" +";" + "Mean surface in ch2"  +";"+ "Mean length in ch2"  +";"+ "Colocalization ch1 in ch2 (signal based)"
						+";" + "Colocalization ch2 in ch1 (signal based)"
						+";" + "Colocalization ch1 in ch2 (size based)"
						+";" + "Colocalization ch2 in ch1 (size based)"
						+";" + "Colocalization ch1 in ch2 (objects numbers)"
						+";" + "Colocalization ch2 in ch1 (objects numbers)"
						+";" + "Mean ch2 intensity of ch1 objects"
						+";" + "Mean ch1 intensity of ch2 objects"
						+";" + "Pearson correlation"
						+";" + "Pearson correlation inside cell masks");
			
				out.println();
				out.flush();
			}
			else
			{
				out.print("File"+ ";" +"Image ID" + ";"+ "Objects ch1" + ";" + "Mean size in ch1"  +";" + "Mean surface in ch1"  +";"+ "Mean length in ch1");
				out.println();
				out.flush();
			}

			out.println();
			out.print(
					"Parameters:" + " " + 
							"background removal " + " "+ Analysis.p.removebackground  + " " +
							"window size " + Analysis.p.size_rollingball + " " +
							"stddev PSF xy " + " "+ mosaic.bregman.Tools.round(Analysis.p.sigma_gaussian, 5) + " " +
							"stddev PSF z " + " "+ mosaic.bregman.Tools.round(Analysis.p.sigma_gaussian/Analysis.p.zcorrec, 5)+ " " +
							"Regularization " + Analysis.p.lreg  + " " +
							"Min intensity ch1 " + Analysis.p.min_intensity +" " +
							"Min intensity ch2 " + Analysis.p.min_intensityY +" " +
							"subpixel " + Analysis.p.subpixel + " " +
							"Cell mask ch1 " + Analysis.p.usecellmaskX + " " +
							"mask threshold ch1 " + Analysis.p.thresholdcellmask + " " +
							"Cell mask ch2 " + Analysis.p.usecellmaskY + " " +
							"mask threshold ch2 " + Analysis.p.thresholdcellmasky + " " +									
							"Intensity estimation " + choice1[Analysis.p.mode_intensity] + " " +
							"Noise model " + choice2[Analysis.p.noise_model]+ ";"
					);
			out.println();
			out.flush();
		}
		
		if (Analysis.p.nchannels == 2)
		{
			double corr_mask, corr, corr_zero;
			double [] temp;
			temp=Analysis.pearson_corr();
			corr=temp[0];
			corr_mask=temp[1];
			corr_zero=temp[2];
			
			out.print(filename + ";" + mosaic.bregman.Tools.round(corr,3) + ";" + mosaic.bregman.Tools.round(corr_mask,3)+ ";" + mosaic.bregman.Tools.round(corr_zero,3));
			out.println();
			out.flush();
			
			double colocAB=mosaic.bregman.Tools.round(Analysis.colocsegAB(hcount),4);
			double colocABnumber = mosaic.bregman.Tools.round(Analysis.colocsegABnumber(),4);
			double colocABsize = mosaic.bregman.Tools.round(Analysis.colocsegABsize(hcount),4);
			double colocBA=mosaic.bregman.Tools.round(Analysis.colocsegBA(out3, hcount),4);
			double colocBAnumber = mosaic.bregman.Tools.round(Analysis.colocsegBAnumber(),4);
			double colocBAsize=mosaic.bregman.Tools.round(Analysis.colocsegBAsize(out3, hcount),4);
			double colocA=mosaic.bregman.Tools.round(Analysis.colocsegA(null),4);
			double colocB=mosaic.bregman.Tools.round(Analysis.colocsegB(null),4);
			
			double meanSA= Analysis.meansurface(Analysis.regionslistA);
			double meanSB= Analysis.meansurface(Analysis.regionslistB);

			double meanLA= Analysis.meanlength(Analysis.regionslistA);
			double meanLB= Analysis.meanlength(Analysis.regionslistB);
			
			out.print(filename + ";" + hcount +";"+ Analysis.na + ";" +
				mosaic.bregman.Tools.round(Analysis.meana , 4)  +";" + 
				mosaic.bregman.Tools.round(meanSA , 4)  +";" +
				mosaic.bregman.Tools.round(meanLA , 4)  +";" +
				+ Analysis.nb +";" + 
				mosaic.bregman.Tools.round(Analysis.meanb , 4) +";" +
				mosaic.bregman.Tools.round(meanSB , 4)  +";" +
				mosaic.bregman.Tools.round(meanLB , 4)  +";" +
				colocAB +";" + 
				colocBA + ";"+
				colocABsize +";" + 
				colocBAsize + ";"+
				colocABnumber +";" + 
				colocBAnumber + ";"+
				colocA+ ";"+
				colocB+ ";"+
				mosaic.bregman.Tools.round(corr, 4) +";"+
				mosaic.bregman.Tools.round(corr_mask, 4)
				);
			out.println();
			out.flush();
		}
		else
		{
			double meanSA= Analysis.meansurface(Analysis.regionslistA);
			double meanLA= Analysis.meanlength(Analysis.regionslistA);
			
			out.print(filename + ";" + hcount +";"+ Analysis.na + ";" +
					mosaic.bregman.Tools.round(Analysis.meana , 4)+";"+
					mosaic.bregman.Tools.round(meanSA , 4)+";"+
					mosaic.bregman.Tools.round(meanLA , 4)
					);
			out.println();
			out.flush();
		}
		return out;
	}
	
	public void Headless_file()
	{
		try
		{
			ImagePlus img = null;
			
			System.out.println("The path is: " + wpath);
			
			if (wpath != null)
			{
				Analysis.p.wd= (new File(wpath)).getParent() +File.separator;
				System.out.println("opening: " + wpath);
				img=IJ.openImage(wpath);
			}
			else
			{
				/* Get Image directory */
				
				Analysis.p.wd = MosaicUtils.ValidFolderFromImage(aImp);
				
				img = aImp;
			}
			
			Analysis.p.nchannels=img.getNChannels();

			if(Analysis.p.save_images)
			{
				String savepath = null;
				//IJ.log(wpath);
				if (wpath != null)
					savepath =  wpath.substring(0,wpath.length()-4);
				else
				{
					savepath = Analysis.p.wd;
				}
				//IJ.log(savepath);


				if(Analysis.p.nchannels==2)
				{
					out3 = new PrintWriter(savepath+"_ObjectsData_c2"+ ".csv");
					
					Analysis.p.file1=savepath+"_ObjectsData_c1"+ ".csv";
					Analysis.p.file2=savepath+"_ObjectsData_c2"+ ".csv";
					Analysis.p.file3=savepath+"_ImagesData"+ ".csv";
					if(Analysis.p.save_images)
					{
						script = new RScript(
								Analysis.p.wd, Analysis.p.file1, Analysis.p.file2, Analysis.p.file3,
								Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames,
								Analysis.p.ch1,Analysis.p.ch2
								);
						script.writeScript();
					}

					if(Analysis.p.nz>1)
					{
						out3.print("Image ID" + ";" + "Object ID" +";"
								+ "Size" + ";" + "Surface" + ";" + "Length" + ";" +"Intensity" + ";"
								+ "Overlap with ch1" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out3.println();
					}
					else
					{
						out3.print("Image ID" + ";" + "Object ID" +";"
								+ "Size" + ";" + "Perimeter" + ";" + "Length" + ";" +"Intensity" + ";"
								+ "Overlap with ch1" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out3.println();		
					}
				}
			}
			//IJ.log("single file start headless");
			//IJ.log("start headless file");
			bcolocheadless(img);
			IJ.log("");
			IJ.log("Done");


			if(Analysis.p.save_images)
			{
				String choice1[] = {"Automatic", "Low layer", "Medium layer","High layer"};
				String choice2[] = {"Poisson", "Gauss"};
				finish();
			}

		}
		catch (Exception e)
		{//Catch exception if any
			e.printStackTrace();
			System.err.println("Error launcher file processing: " + e.getMessage());
		}

	}

	public void Headless_directory(){
		//IJ.log("starting dir");
		try
		{
			wpath=wpath + File.separator;
			Analysis.p.wd= wpath;
			Analysis.doingbatch=true;

			Analysis.p.livedisplay=false;

			Analysis.p.dispwindows=false;
			Analysis.p.save_images=true;

			IJ.log(Analysis.p.wd);
			//long Time = new Date().getTime(); //start time

			String [] list = new File(wpath).list();
			if (list==null) {IJ.log("No files in folder"); return;}
			Arrays.sort(list);

			//IJ.log("la");
			int ii=0;
			boolean imgfound=false;
			while (ii<list.length && !imgfound) 
			{
				if(Analysis.p.debug){IJ.log("read"+list[ii]);}
				boolean isDir = (new File(wpath+list[ii])).isDirectory();
				if (	!isDir &&
						!list[ii].startsWith(".")&&
						!list[ii].startsWith("Coloc") &&
						!list[ii].startsWith("X_Vesicles")&&
						!list[ii].startsWith("Y_Vesicles")&&
						!list[ii].endsWith("_seg_c1.tif")&&
						!list[ii].endsWith("_seg_c2.tif")&&
						!list[ii].endsWith("_mask_c1.tif")&&
						!list[ii].endsWith("_mask_c2.tif")&&
						!list[ii].endsWith("_ImageData.tif")&&
						!list[ii].endsWith(".zip")&&
						(list[ii].endsWith(".tif") || list[ii].endsWith(".tiff") ) 
						){

					ImagePlus img=IJ.openImage(wpath+list[ii]);
					Analysis.p.nchannels=img.getNChannels();
					imgfound=true;

				}
				ii++;
			}


			//IJ.log("nchannels" + Analysis.p.nchannels);

			if(Analysis.p.nchannels==2)
			{
				IJ.log("looking for files at " + wpath);
				String [] directrories=  wpath.split("\\"+File.separator);
				int nl = directrories.length;
				String savepath=(directrories[nl-1]).replaceAll("\\"+File.separator, ""); 

//				out  = new PrintWriter(wpath+savepath+"_ImageData"+ ".csv");
				out3 = new PrintWriter(wpath+savepath+"_ObjectsData_c2"+ ".csv");

				Analysis.p.file1=wpath+savepath+"_ObjectsData_c1"+ ".csv";
				Analysis.p.file2=wpath+savepath+"_ObjectsData_c2"+ ".csv";
				Analysis.p.file3=wpath+savepath+"_ImagesData"+ ".csv";
				script = new RScript(
						Analysis.p.wd, Analysis.p.file1, Analysis.p.file2, Analysis.p.file3,
						Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames,
						Analysis.p.ch1,Analysis.p.ch2
						);
				script.writeScript();
				
				
				if(Analysis.p.nz>1)
				{
					out3.print("Image ID" + ";" + "Object ID" +";"
							+ "Size" + ";" + "Surface" + ";" + "Length" + ";" +"Intensity" + ";"
							+ "Overlap with ch1" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
					out3.println();
				}
				else
				{
					out3.print("Image ID" + ";" + "Object ID" +";"
							+ "Size" + ";" + "Perimeter" + ";" + "Length" + ";" +"Intensity" + ";"
							+ "Overlap with ch1" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
					out3.println();		
				}
			}

			else
			{

				String [] directrories=  wpath.split("\\"+File.separator);
				int nl = directrories.length;
				String savepath=(directrories[nl-1]).replaceAll("\\"+File.separator, ""); 
/*				out  = new PrintWriter(wpath+savepath+"_Images_data"+ ".csv");

				out.println();
				out.print("File"+ ";" +"Image ID" + ";"+ "Objects ch1" + ";" + "Mean size in ch 1" + ";" + "Mean surface in ch1"  +";"+ "Mean length in ch1"  );
				out.println();*/
			}

			for (int i=0; i<list.length; i++) {
				if(Analysis.p.debug){IJ.log("read"+list[i]);}
				boolean isDir = (new File(wpath+list[i])).isDirectory();
				if (	!isDir &&
						!list[i].startsWith(".") &&
						!list[i].startsWith("Coloc") &&
						!list[i].startsWith("X_Vesicles")&&
						!list[i].startsWith("Objects_data")&&
						!list[i].startsWith("Y_Vesicles")&&
						!list[i].endsWith("_seg_c1.tif")&&
						!list[i].endsWith("_seg_c2.tif")&&
						!list[i].endsWith("_mask_c1.tif")&&
						!list[i].endsWith("_mask_c2.tif")&&
						!list[i].endsWith("_ImageData.tif")&&
						list[i].endsWith(".tif")&&
						!list[i].endsWith(".zip")
						){
					IJ.log("Analyzing " + list[i]+ "... ");
					ImagePlus img=IJ.openImage(wpath+list[i]);
					//IJ.log("opened");

					if(Analysis.p.pearson)
						bcolocheadless_pearson(img);
					else
						bcolocheadless(img);
					//IJ.log("done");

					Runtime.getRuntime().gc();
				}
			}
			IJ.log("");
			IJ.log("Done");

			finish();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error headless: " + e.getMessage());
		}
		Analysis.doingbatch=false;
	}


	public void bcolocheadless_pearson(ImagePlus img2){
		double Ttime=0;
		long lStartTime = new Date().getTime(); //start time

		//Analysis.p.livedisplay=false;

		Analysis.p.blackbackground=ij.Prefs.blackBackground;
		ij.Prefs.blackBackground=false;
		Analysis.p.nchannels=img2.getNChannels();

		//IJ.log("dialog j" + ij.Prefs.useJFileChooser);

		if(Analysis.p.nchannels==2){
			Analysis.load2channels(img2);
		}


		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		long lEndTime = new Date().getTime(); //start time

		long difference = lEndTime - lStartTime; //check different
		Ttime +=difference;
		IJ.log("Total Time : " + Ttime/1000 + "s");

	}


	/* 
	 *  It segment the image and give co-localization analysis result
	 *  for a 2 channel image
	 *  
	 *  @param img2 Image to segment and analyse
	 * 
	 */
	
	public void bcolocheadless(ImagePlus img2)
	{
		double Ttime=0;
		long lStartTime = new Date().getTime(); //start time

		//Analysis.p.livedisplay=false;

		Analysis.p.blackbackground=ij.Prefs.blackBackground;
		ij.Prefs.blackBackground=false;
		Analysis.p.nchannels=img2.getNChannels();

		//IJ.log("dialog j" + ij.Prefs.useJFileChooser);

		if(Analysis.p.nchannels==2)
		{
			Analysis.load2channels(img2);
		}

		if(Analysis.p.nchannels==1)
		{
			Analysis.load1channel(img2);
		}

		//Analysis.p.dispvoronoi=true;
		//Analysis.p.livedisplay=true;
		if(Analysis.p.mode_voronoi2)
		{
			if(Analysis.p.nz>1)
			{
				Analysis.p.max_nsb=151;
				Analysis.p.interpolation=2;
			}else
			{
				Analysis.p.max_nsb=151;
				Analysis.p.interpolation=4;
			}
		}

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		//IJ.log("dispcolors" + Analysis.p.dispcolors);
		Analysis.segmentA();			 

		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}


		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//IJ.log("imgb :" +list[i+1]);

		if(Analysis.p.nchannels==2){
			Analysis.segmentb();			 

			try {
				Analysis.DoneSignalb.await();
			}catch (InterruptedException ex) {}
		}


		//TODO : why is it needed to reassign p.ni ...??
		Analysis.p.ni=Analysis.imgA.getWidth();
		Analysis.p.nj=Analysis.imgA.getHeight();
		Analysis.p.nz=Analysis.imgA.getNSlices();



		if(Analysis.p.nchannels==2)
		{
			//IJ.log("computemask");
			Analysis.computeOverallMask();
			//IJ.log("1");
			Analysis.regionslistA=Analysis.removeExternalObjects(Analysis.regionslistA);
			//IJ.log("2");
			Analysis.regionslistB=Analysis.removeExternalObjects(Analysis.regionslistB);

			//IJ.log("setriongslabels");
			Analysis.setRegionsLabels(Analysis.regionslistA, Analysis.regionsA);
			Analysis.setRegionsLabels(Analysis.regionslistB, Analysis.regionsB);
			int factor2 =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
			int fz2;
			if(Analysis.p.nz>1)fz2=factor2; else fz2=1;

			MasksDisplay md= new MasksDisplay(Analysis.p.ni*factor2,Analysis.p.nj*factor2,Analysis.p.nz*fz2,Analysis.p.nlevels,Analysis.p.cl,Analysis.p);
			md.displaycoloc(Analysis.regionslistA,Analysis.regionslistB);


			if(Analysis.p.dispoutline)
			{
				//IJ.log("disp outline");
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				int fz;
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				displayoutline(Analysis.regionsA, Analysis.imagea,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1);
				displayoutline(Analysis.regionsB, Analysis.imageb,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 2);
			}
			if(Analysis.p.dispint)
			{
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				//IJ.log("factor" + factor);
				int fz;
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				displayintensities(Analysis.regionslistA, Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1, Analysis.imagecolor_c1);
				displayintensities(Analysis.regionslistB, Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 2, Analysis.imagecolor_c2);
			}

			//			if(Analysis.p.save_images){
			//				Analysis.setIntensitiesandCenters(Analysis.regionslistA, Analysis.imagea);
			//				Analysis.setIntensitiesandCenters(Analysis.regionslistB, Analysis.imageb);
			//
			//				Analysis.setPerimeter(Analysis.regionslistA,Analysis.regionsA);	
			//				Analysis.setPerimeter(Analysis.regionslistB,Analysis.regionsB);	
			//
			//				if(Analysis.p.nz==1){
			//					Analysis.setlength(Analysis.regionslistA,Analysis.regionsA);
			//					Analysis.setlength(Analysis.regionslistB,Analysis.regionsB);
			//				}
			//			}
			//IJ.log("na");
			Analysis.na=Analysis.regionslistA.size();
			Analysis.nb=Analysis.regionslistB.size();

			Analysis.meana=Analysis.meansize(Analysis.regionslistA);
			Analysis.meanb=Analysis.meansize(Analysis.regionslistB);

			//IJ.log("f");

			//if(Analysis.p.dispwindows){
			IJ.log("Colocalization ch1 in ch2: " +mosaic.bregman.Tools.round(colocsegAB,4));
			IJ.log("Colocalization ch2 in ch1: " +mosaic.bregman.Tools.round(colocsegBA,4));
			//}
			if(Analysis.p.save_images)
			{
				Analysis.printobjectsB(out3, hcount);
				out3.flush();
			}

			Analysis.doingbatch=false;
			hcount++;

		}


		if(Analysis.p.nchannels==1)
		{
			if(Analysis.p.dispoutline)
			{
				//IJ.log("disp outline");
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				int fz;
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				//				long lStartTime = new Date().getTime(); //start time
				displayoutline(Analysis.regionsA, Analysis.imagea,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1);
				//				long lEndTime = new Date().getTime(); //start time
				//				long difference = lEndTime - lStartTime; //check different
				//				IJ.log("Elapsed milliseconds dispoutl: " + difference);
			}

			if(Analysis.p.dispint)
			{
				//	IJ.log("disp int");
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				int fz;
				//	IJ.log("factor" + factor);
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				//				long lStartTime = new Date().getTime(); //start time
				displayintensities(Analysis.regionslistA, Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1, Analysis.imagecolor_c1);
				//				long lEndTime = new Date().getTime(); //start time
				//				long difference = lEndTime - lStartTime; //check different
				//				IJ.log("Elapsed milliseconds dispintsts: " + difference);
			}
			//	Analysis.setRegionsLabels(Analysis.regionslistA, Analysis.regionsA);
			//			long lStartTime = new Date().getTime(); //start time
			//IJ.log("analysis");
			Analysis.na=Analysis.regionslistA.size();
			//IJ.log("intensities");

			//IJ.log("mean size");
			Analysis.meana=Analysis.meansize(Analysis.regionslistA);

			double meanSA= Analysis.meansurface(Analysis.regionslistA);			
			double meanLA= Analysis.meanlength(Analysis.regionslistA);

			//IJ.log("save");
			if(Analysis.p.save_images)
			{
				String savepath = null;
				//IJ.log(wpath);
				if (wpath != null)
					savepath =  wpath.substring(0,wpath.lastIndexOf(File.separator)+1);
				else
				{
					savepath = Analysis.p.wd;
				}

				//IJ.log("print objects");
//				Analysis.printobjects(out2, hcount);
				
				boolean append = false;
				
				if (hcount == 0)
					append = false;
				else
					append = true;
				
				Vector<?> obl = Analysis.getObjectsList(hcount);
				
				String filename_without_ext = img2.getTitle().substring(0, img2.getTitle().lastIndexOf("."));
				
				InterPluginCSV<?> IpCSV = CSVOutput.getInterPluginCSV();
				IpCSV.setMetaInformation("background", savepath + File.separator + img2.getTitle());
				
				System.out.println(savepath + File.separator +  filename_without_ext + "_ObjectsData_c1" + ".csv");
				IpCSV.Write(savepath + File.separator +  filename_without_ext + "_ObjectsData_c1" + ".csv",obl,CSVOutput.occ, append);
			}

			hcount++;

		}
		ij.Prefs.blackBackground=Analysis.p.blackbackground;


		long lEndTime = new Date().getTime(); //start time

		long difference = lEndTime - lStartTime; //check different
		Ttime +=difference;
		IJ.log("Total Time : " + Ttime/1000 + "s");

	}

	ImagePlus out_over[] = new ImagePlus[2];
	
	/**
	 * 
	 * Display outline overlay segmentation
	 * 
	 * @param regions mask with intensities
	 * @param image image
	 * @param dz z size
	 * @param di x size
	 * @param dj y size
	 * @param channel (?)
	 */
	
	public void displayoutline(short [][][] regions, double [][][] image, int dz, int di, int dj, int channel)
	{
		ImageStack objS;
		ImagePlus objcts= new ImagePlus();


		//build stack and imageplus for objects
		objS=new ImageStack(di,dj);

		for (int z=0; z<dz; z++)
		{
			byte[] mask_bytes = new byte[di*dj];
			for (int i=0; i<di; i++) 
			{  
				for (int j=0; j<dj; j++)
				{
					if(regions[z][i][j]> 0)
						mask_bytes[j * di + i]= 0;
					else
					mask_bytes[j * di + i]=(byte) 255;
				}
			}

			ByteProcessor bp = new ByteProcessor(di,dj);
			bp.setPixels(mask_bytes);
			objS.addSlice("", bp);

		}
		objcts.setStack("Objects",objS);


		//build image in bytes
		ImageStack imgS;
		ImagePlus img= new ImagePlus();


		//build stack and imageplus for the image
		imgS=new ImageStack(Analysis.p.ni,Analysis.p.nj);

		for (int z=0; z<Analysis.p.nz; z++)
		{
			byte[] mask_bytes = new byte[Analysis.p.ni*Analysis.p.nj];
			for (int i=0; i<Analysis.p.ni; i++) 
			{  
				for (int j=0; j<Analysis.p.nj; j++) 
				{  
					mask_bytes[j * Analysis.p.ni + i]=(byte) ((int) 255*image[z][i][j]);
				}
			}

			ByteProcessor bp = new ByteProcessor(Analysis.p.ni,Analysis.p.nj);
			bp.setPixels(mask_bytes);

			imgS.addSlice("", bp);
		}
		
		img.setStack("Image",imgS);

		//resize z
		Resizer re = new Resizer();
		img=re.zScale(img, dz, ImageProcessor.NONE);
		//img.duplicate().show();
		ImageStack imgS2=new ImageStack(di,dj);
		for (int z=0; z<dz; z++)
		{
			img.setSliceWithoutUpdate(z+1);
			img.getProcessor().setInterpolationMethod(ImageProcessor.NONE);
			imgS2.addSlice("",img.getProcessor().resize(di, dj, false));
		}
		img.setStack(imgS2);

		for (int z=1; z<=dz; z++)
		{
			BinaryProcessor  bip = new BinaryProcessor((ByteProcessor) objcts.getStack().getProcessor(z));
			bip.outline();
			bip.invert();
		}
		
		// if we have already an outline overlay image merge the frame
		
		if (out_over[channel-1] != null)
		{
			ImagePlus tab []= new ImagePlus [2];
			tab[0]=objcts;tab[1]=img;
			ImagePlus over = RGBStackMerge.mergeChannels(tab, false);
			MosaicUtils.MergeFrames(out_over[channel-1],over);
		}
		else
		{
			ImagePlus tab []= new ImagePlus [2];
			tab[0]=objcts;tab[1]=img;
			out_over[channel-1] =RGBStackMerge.mergeChannels(tab, false);
		}

		if(Analysis.p.dispwindows)
		{
			out_over[channel-1].setTitle("Objects outlines, channel " + channel);
			out_over[channel-1].show();
		}
	}

	ImagePlus out_disp[] = new ImagePlus[2];

	/**
	 * 
	 * Display intensity result
	 * 
	 * @param regionslist Regions
	 * @param dz image size z
	 * @param di image size x
	 * @param dj image size y
	 * @param channel
	 * @param imagecolor
	 */
	
	public void displayintensities(ArrayList<Region> regionslist,int dz, int di, int dj, int channel, byte [] imagecolor)
	{
		ImageStack intS;
		ImagePlus intensities= new ImagePlus();

		//build stack and imageplus
		intS=new ImageStack(di,dj);
		for (int z=0; z<dz; z++) {  
			int [] tabt= new int [3];

			ColorProcessor cpcoloc= new ColorProcessor(di,dj);
			for (int i=0;i<di;i++) {
				int t=z*di*dj*3+i*dj*3;
				for (int j=0;j< dj;j++){
					tabt[0]=imagecolor[t+j*3+0] & 0xFF;
					tabt[1]=imagecolor[t+j*3+1] & 0xFF;
					tabt[2]=imagecolor[t+j*3+2] & 0xFF;
					cpcoloc.putPixel(i, j, tabt);
				}
			}
			intS.addSlice("Intensities reconstruction, channel " + channel, cpcoloc);

		}
		intensities.setStack("Intensities reconstruction, channel " +channel, intS);
		
		
		if (out_disp[channel-1] != null)
		{
			MosaicUtils.MergeFrames(out_disp[channel-1],intensities);
		}
		else
		{
			out_disp[channel-1] = intensities;
		}
		
		if(Analysis.p.dispwindows)
		{
			out_disp[channel-1].show();
			out_disp[channel-1].setStack(out_disp[channel-1].getStack());
		}
	}

	public  void finish(){
		if(Analysis.p.save_images)
		{
			if(Analysis.p.nchannels==2)
			{
				out3.close();
			}
		}
	}



}
