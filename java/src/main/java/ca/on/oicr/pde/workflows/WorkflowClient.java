package ca.on.oicr.pde.workflows;

import ca.on.oicr.pde.utilities.workflows.OicrWorkflow;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.seqware.pipeline.workflowV2.model.Command;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;

public class WorkflowClient extends OicrWorkflow {

    private static final Logger logger = Logger.getLogger(WorkflowClient.class.getName());
    //workflow parameters
    //private String /extract = null;
    //private boolean doExtract = false;
    private String queue = null;
    private String inputFiles = null;
    //private String outputPrefix = null;
    //private String outputDir = null;
   // private String outputPath = null;
    //workflow programs
    private String perl = null;
    private String java = null;
    private String fastqc = null;
    //workflow directories
    private String binDir = null;
    private String dataDir = null;
    //private String finalOutputDir = null;
    private Boolean manualOutput = false;
    

    //Constructor - called in setupDirectory()
    private void WorkflowClient() {

        binDir = getWorkflowBaseDir() + "/bin/";
        dataDir = "data/";

//        //load workflow parameters
        try {
//            outputDir = getProperty("output_dir");
//            outputPrefix = getProperty("output_prefix");
//            outputPath = getProperty("output_path");
//
//            if (Arrays.asList("na", "").contains(outputPath.toLowerCase().trim())) {
//                outputPath = outputPrefix + outputDir + "/seqware-" + getSeqware_version() + "_" + getName() + "_" + getVersion() + "/" + getRandom() + "/";
//            } else {
//                //make sure the path ends with a "/"
//                outputPath = outputPath.lastIndexOf("/") == (outputPath.length() - 1) ? outputPath : outputPath + "/";
////                finalOutputDir = outputPath;
////            }
            
            manualOutput = Boolean.valueOf(getProperty("manual_output"));
            perl = binDir + getProperty("perl");
            java = binDir + getProperty("java");
            fastqc = binDir + getProperty("fastqc");
            //extract = getProperty("extract");        
            //doExtract = extract.equals("yes") ? true : false;
            queue = getOptionalProperty("queue", "");
            inputFiles = getProperty("input_files");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Expected parameter missing", ex);
        }

    }

    @Override
    public void setupDirectory() {

        WorkflowClient(); //Constructor call
        addDirectory(dataDir);
        //addDirectory(outputPath);

    }

    @Override
    public Map<String, SqwFile> setupFiles() {
        
        int fileNumber = 0;
        for (String inputFilePath : inputFiles.split(",")) {
            SqwFile file = this.createFile("file_in_" + fileNumber++);
            file.setSourcePath(inputFilePath);
            file.setType("chemical/seq-na-fastq");
            file.setIsInput(true);
        }
        return this.getFiles();

    }

    @Override
    public void buildWorkflow() {

        //Launch a fastq qc job for each input file
        for (Map.Entry<String, SqwFile> file : this.getFiles().entrySet()) {
            logger.info(String.format("%s %s", file.getKey(), file.getValue().getProvisionedPath()));
            Job job = getFastQcJob(file.getValue().getProvisionedPath());
            job.setMaxMemory("4000");
            job.setQueue(queue);
        }

    }

    private Job getFastQcJob(String fastqInputFilePath) {

        Job job = getWorkflow().createBashJob("FastQC");
        Command command = job.getCommand();
        command.addArgument(perl);
        command.addArgument(fastqc);
        //command.addArgument(getFiles().get("file_in_0").getProvisionedPath());
        command.addArgument(fastqInputFilePath);
        command.addArgument("--java=" + java);

//        //extraction of fastqc report zip disabled
//        if (!doExtract) {
        command.addArgument("--noextract");
//        }

        command.addArgument("--outdir " + dataDir);

        String outputFileName = fastqInputFilePath.substring(fastqInputFilePath.lastIndexOf("/") + 1, fastqInputFilePath.lastIndexOf(".fastq")) + "_fastqc.zip";
        //SqwFile sqwOutputFile = createOutFile(dataDir + outputFileName, "application/zip-report-bundle", finalOutputDir + outputFileName, true);
        SqwFile sqwOutputFile = createOutputFile(dataDir + outputFileName, "application/zip-report-bundle", manualOutput);
        job.addFile(sqwOutputFile);

//        //extraction of fastqc report zip disabled
//        if(doExtract){
//            String extractedOutputDir = outputFile.substring(0, outputFile.lastIndexOf(".zip")) + "/";
//            SqwFile sqwExtractedOutputDir = createOutFile(dataDir + extractedOutputDir, "", outputDir + extractedOutputDir + dataDir, true);  //broken - does not provision directory recursively
//            job.addFile(sqwExtractedOutputDir);
//        }

        return job;

    }

//    private SqwFile createOutFile(String sourcePath, String sourceType, String outputPath, boolean forceCopy) {
//
//        SqwFile file = createOutputFile(sourcePath, sourceType, manualOutput);
////        file.setSourcePath(sourcePath);
////        file.setType(sourceType);
////        file.setIsOutput(true);
////        file.setOutputPath(outputPath);
////        file.setForceCopy(forceCopy);
////
////        return file;
//
//    }
    
    
}
