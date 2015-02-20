package ca.on.oicr.pde.workflows;

import ca.on.oicr.pde.utilities.workflows.OicrWorkflow;
import java.util.Map;
import java.util.logging.Logger;
import net.sourceforge.seqware.pipeline.workflowV2.model.Command;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;

public class WorkflowClient extends OicrWorkflow {

    private static final Logger logger = Logger.getLogger(WorkflowClient.class.getName());

    //workflow parameters
    private String queue = null;
    private String inputFiles = null;
    private Boolean manualOutput = false;

    //workflow programs
    private String perl = null;
    private String java = null;
    private String fastqc = null;

    //workflow directories
    private String binDir = null;
    private String dataDir = null;

    //Constructor - called in setupDirectory()
    private void WorkflowClient() {
        binDir = getWorkflowBaseDir() + "/bin/";
        dataDir = "data/";
        manualOutput = Boolean.valueOf(getProperty("manual_output"));
        perl = binDir + getProperty("perl");
        java = binDir + getProperty("java");
        fastqc = binDir + getProperty("fastqc");
        queue = getOptionalProperty("queue", "");
        inputFiles = getProperty("input_files");
    }

    @Override
    public void setupDirectory() {
        WorkflowClient(); //Constructor call
        addDirectory(dataDir);
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
        command.addArgument(fastqInputFilePath);
        command.addArgument("--java=" + java);
        command.addArgument("--noextract");
        command.addArgument("--outdir " + dataDir);

        String outputFileName = fastqInputFilePath.substring(fastqInputFilePath.lastIndexOf("/") + 1, fastqInputFilePath.lastIndexOf(".fastq")) + "_fastqc.html";
        SqwFile sqwOutputFile = createOutputFile(dataDir + outputFileName, "text/html", manualOutput);
        job.addFile(sqwOutputFile);

        return job;
    }

}
