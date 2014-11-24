package ca.on.oicr.pde.deciders;

import ca.on.oicr.pde.dao.reader.SeqwareReadService;
import ca.on.oicr.pde.model.SeqwareObject;
import ca.on.oicr.pde.model.Workflow;
import ca.on.oicr.pde.testing.metadata.RegressionTestStudy;
import ca.on.oicr.pde.dao.writer.SeqwareWriteService;
import ca.on.oicr.pde.dao.writer.SeqwareWriteService.FileInfo;
import ca.on.oicr.pde.utilities.Helpers;
import ca.on.oicr.pde.dao.executor.SeqwareExecutor;
import ca.on.oicr.pde.model.Ius;
import ca.on.oicr.pde.model.WorkflowRunReportRecord;
import ca.on.oicr.pde.testing.decider.DeciderRunTestReport;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 *
 * @author mlaszloffy
 */
public class FastqQCDeciderIT {

    RegressionTestStudy r;
    File bundledWorkflow;
    SeqwareExecutor ses;
    SeqwareReadService srs;
    SeqwareWriteService sws;
    Map<String, SeqwareObject> sos;

    public FastqQCDeciderIT() {

    }

    @BeforeSuite
    public void setupMetadb() {

        //get the workflow bundle associated with the decider
        bundledWorkflow = Helpers.getBundledWorkflow();
        Assert.assertNotNull(bundledWorkflow, "Unable to locate the workflow bundle.");
        Assert.assertTrue(bundledWorkflow.exists(), "The workflow bundle [" + bundledWorkflow + "] does not exist.");

        //get the database settings
        String dbHost = System.getProperty("dbHost");
        String dbPort = System.getProperty("dbPort");
        String dbUser = System.getProperty("dbUser");
        String dbPassword = System.getProperty("dbPassword");
        assertNotNull(dbHost, "Set dbHost to a testing Postgres database host name");
        assertNotNull(dbPort, "Set dbPort to a testing Postgres database port");
        assertNotNull(dbUser, "Set dbUser to a testing Postgres database user name");
        assertNotNull(dbPassword, "Set dbPassword to a testing Postgres database password");
        
        //get the seqware webservice war
        String seqwareWarPath = System.getProperty("seqwareWar");
        assertNotNull(seqwareWarPath, "seqwareWar is not set.");
        File seqwareWar = new File(seqwareWarPath);
        assertTrue(seqwareWar.exists(), "seqware was is not accessible.");

        //get the regression test study and PDE's service objects
        r = new RegressionTestStudy(dbHost, dbPort, dbUser, dbPassword, seqwareWar);
        ses = r.getSeqwareExecutor();
        srs = r.getSeqwareReadService();
        sws = r.getSeqwareWriteService();
        sos = r.getSeqwareObjects();

        //set the seqware settings path (needed by Seqware's plugin runner)
        System.setProperty("SEQWARE_SETTINGS", r.getSeqwareSettings().getAbsolutePath());
    }

    @BeforeClass
    public void addFiles() {
        //Install base caller to generate fastqs
        Workflow upstreamWorkflow = sws.createWorkflow("CASAVA", "0.0", "");

        //get all ius objects and link two fastqs
        for (Ius ius : Iterables.filter(sos.values(), Ius.class)) {
            sws.createWorkflowRun(upstreamWorkflow, Arrays.asList(ius),
                    Arrays.asList(new FileInfo("type", "chemical/seq-na-fastq-gzip", "/tmp/" + ius.getSwid() + ".R1.fastq.gz"),
                            new FileInfo("type", "chemical/seq-na-fastq-gzip", "/tmp/" + ius.getSwid() + ".R2.fastq.gz")));
        }
    }

    @Test
    public void basicScheduleTest() throws IOException {

        //install the workflow
        //TODO: installWorkflow should return a workflow...
        Workflow.Builder b = new Workflow.Builder();
        b.setName("");
        b.setVersion("");
        b.setSwid(ses.installWorkflow(Helpers.getBundledWorkflow()).getSwid());
        Workflow fastqcWorkflow = b.build();

        run(fastqcWorkflow, "--all");
        DeciderRunTestReport report = DeciderRunTestReport.generateReport(srs, fastqcWorkflow, Collections.EMPTY_LIST);
        Assert.assertEquals(report.getMaxInputFiles().intValue(), 2);
        Assert.assertEquals(report.getMinInputFiles().intValue(), 2);
        Assert.assertEquals(report.getWorkflowRunCount().intValue(), 16);
        Assert.assertEquals(report.getFileMetaTypes(), Sets.newHashSet("chemical/seq-na-fastq-gzip"));

        //all fastqs have been scheduled for processing, no new workflow runs should be schedulable
        run(fastqcWorkflow, "--all");
        DeciderRunTestReport report2 = DeciderRunTestReport.generateReport(srs, fastqcWorkflow, Collections.EMPTY_LIST);
        Assert.assertEquals(report2.getWorkflowRunCount().intValue(), 16);
        Assert.assertEquals(report, report2);
    }

    @Test
    public void launchMaxTest() throws IOException {

        Workflow.Builder b = new Workflow.Builder();
        b.setName("");
        b.setVersion("");
        b.setSwid(ses.installWorkflow(Helpers.getBundledWorkflow()).getSwid());
        Workflow fastqcWorkflow = b.build();

        DeciderRunTestReport report;

        //schedule 4 runs
        run(fastqcWorkflow, "--all", "--launch-max 4");
        report = DeciderRunTestReport.generateReport(srs, fastqcWorkflow, Collections.EMPTY_LIST);
        Assert.assertEquals(report.getWorkflowRunCount().intValue(), 4);

        //schedule 4 more runs
        run(fastqcWorkflow, "--all", "--launch-max 4");
        report = DeciderRunTestReport.generateReport(srs, fastqcWorkflow, Collections.EMPTY_LIST);
        Assert.assertEquals(report.getWorkflowRunCount().intValue(), 8);

        //shouldn't schedule anything
        run(fastqcWorkflow, "--all", "--launch-max 0");
        report = DeciderRunTestReport.generateReport(srs, fastqcWorkflow, Collections.EMPTY_LIST);
        Assert.assertEquals(report.getWorkflowRunCount().intValue(), 8);

        //schedule the rest
        run(fastqcWorkflow, "--all", "--launch-max 20");
        report = DeciderRunTestReport.generateReport(srs, fastqcWorkflow, Collections.EMPTY_LIST);
        Assert.assertEquals(report.getWorkflowRunCount().intValue(), 16);
    }

    private List<WorkflowRunReportRecord> run(Workflow workflow, String... extraDeciderParams) throws IOException {
        //update the file provenance report (needed by decider)
        sws.updateFileReport();

        //run the decider
        ses.deciderRunSchedule(FastqQCDecider.class.getCanonicalName(), workflow, extraDeciderParams);

        //get workflow runs
        srs.update();
        srs.updateWorkflowRunRecords(workflow); //update() should handle this, but it doesn't... so manually update workflow run records
        return srs.getWorkflowRunRecords(workflow);
    }

    @AfterClass
    public void cleanUp() {
        r.shutdown();
    }

}
