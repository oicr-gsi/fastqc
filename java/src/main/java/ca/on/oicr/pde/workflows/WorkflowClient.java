package ca.on.oicr.pde.workflows;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.seqware.pipeline.workflowV2.AbstractWorkflowDataModel;
import net.sourceforge.seqware.pipeline.workflowV2.model.Command;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;

public class WorkflowClient extends AbstractWorkflowDataModel {

    private static final Logger logger = Logger.getLogger(WorkflowClient.class.getName());
    //workflow parameters
    //private String extract = null;
    //private boolean doExtract = false;
    private String queue = null;
    private String inputFile = null;
    private String outputPrefix = null;
    private String outputDir = null;
    private String outputPath = null;
    //workflow programs
    private String perl = null;
    private String java = null;
    private String fastqc = null;
    //workflow directories
    private String binDir = null;
    private String dataDir = null;
    private String finalOutputDir = null;

    //Constructor - called in setupDirectory()
    private void WorkflowClient() {

        binDir = getWorkflowBaseDir() + "/bin/";
        dataDir = "data/";

        //load workflow parameters
        try {
            outputDir = getProperty("output_dir");
            outputPrefix = getProperty("output_prefix");
            outputPath = getProperty("output_path");
            
            if (Arrays.asList("na", "").contains(outputPath.toLowerCase().trim())) {
                finalOutputDir = outputPrefix + outputDir + "/seqware-" + getSeqware_version() + "_" + getName() + "_" + getVersion() + "/" + getRandom() + "/";
            } else {
                //make sure the path ends with a "/"
                outputPath = outputPath.lastIndexOf("/") == (outputPath.length() - 1) ? outputPath : outputPath + "/";
                finalOutputDir = outputPath;
            }
            
            perl = binDir + getProperty("perl");
            java = binDir + getProperty("java");
            fastqc = binDir + getProperty("fastqc");
            //extract = getProperty("extract");        
            //doExtract = extract.equals("yes") ? true : false;
            queue = getProperty("queue");
            inputFile = getProperty("input_file");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Expected parameter missing", ex);
        }

    }

    @Override
    public void setupDirectory() {

        WorkflowClient(); //Constructor call
        addDirectory(dataDir);
        addDirectory(finalOutputDir);

    }

    @Override
    public Map<String, SqwFile> setupFiles() {

        SqwFile file0 = this.createFile("file_in_0");
        file0.setSourcePath(inputFile);
        file0.setType("chemical/seq-na-fastq");
        file0.setIsInput(true);

        return this.getFiles();

    }

    @Override
    public void buildWorkflow() {

        Job job00 = getFastQcJob();
        job00.setMaxMemory("4000");
        job00.setQueue(queue);

    }

    private Job getFastQcJob() {

        Job job = getWorkflow().createBashJob("FastQC");
        Command command = job.getCommand();
        command.addArgument(perl);
        command.addArgument(fastqc);
        command.addArgument(getFiles().get("file_in_0").getProvisionedPath());
        command.addArgument("--java=" + java);

//        //extraction of fastqc report zip disabled
//        if (!doExtract) {
        command.addArgument("--noextract");
//        }

        command.addArgument("--outdir " + dataDir);

        String outputFile = inputFile.substring(inputFile.lastIndexOf("/") + 1, inputFile.lastIndexOf(".fastq")) + "_fastqc.zip";
        SqwFile sqwOutputFile = createOutFile(dataDir + outputFile, "application/zip-report-bundle", finalOutputDir + outputFile, true);
        job.addFile(sqwOutputFile);

//        //extraction of fastqc report zip disabled
//        if(doExtract){
//            String extractedOutputDir = outputFile.substring(0, outputFile.lastIndexOf(".zip")) + "/";
//            SqwFile sqwExtractedOutputDir = createOutFile(dataDir + extractedOutputDir, "", outputDir + extractedOutputDir + dataDir, true);  //broken - does not provision directory recursively
//            job.addFile(sqwExtractedOutputDir);
//        }

        return job;

    }

    private SqwFile createOutFile(String sourcePath, String sourceType, String outputPath, boolean forceCopy) {

        SqwFile file = new SqwFile();
        file.setSourcePath(sourcePath);
        file.setType(sourceType);
        file.setIsOutput(true);
        file.setOutputPath(outputPath);
        file.setForceCopy(forceCopy);

        return file;

    }
}
