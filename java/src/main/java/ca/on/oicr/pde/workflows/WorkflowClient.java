package ca.on.oicr.pde.workflows;

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
    private String p_outputPrefix = null;
    private String p_outputDir = null;
    //workflow programs
    private String perl = null;
    private String java = null;
    private String fastqc = null;
    //workflow directories
    private String binDir = null;
    private String dataDir = null;
    private String outputDir = null;

    //Constructor - called in setupDirectory()
    private void WorkflowClient() {

        binDir = getWorkflowBaseDir() + "/bin/";
        dataDir = "data/";

        //load workflow parameters
        try {
            p_outputDir = getProperty("output_dir");
            p_outputPrefix = getProperty("output_prefix");
            outputDir = p_outputPrefix + p_outputDir + "/seqware-" + getSeqware_version() + "_" + getName() + "_" + getVersion() + "/" + getRandom() + "/";
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
        addDirectory(outputDir);

    }

    @Override
    public Map<String, SqwFile> setupFiles() {

        try {

            // register an input file
            SqwFile file0 = this.createFile("file_in_0");
            file0.setSourcePath(inputFile);
            file0.setType("chemical/seq-na-fastq");
            file0.setIsInput(true);

            return this.getFiles();

        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return null;
        }

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
        command.addArgument(inputFile);
        command.addArgument("--java=" + java);

//        //extraction of fastqc report zip disabled
//        if (!doExtract) {
        command.addArgument("--noextract");
//        }

        command.addArgument("--outdir " + dataDir);

        String outputFile = inputFile.substring(inputFile.lastIndexOf("/") + 1, inputFile.lastIndexOf(".fastq")) + "_fastqc.zip";
        SqwFile sqwOutputFile = createOutFile(dataDir + outputFile, "application/zip-report-bundle", outputDir + dataDir + outputFile, true);
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
