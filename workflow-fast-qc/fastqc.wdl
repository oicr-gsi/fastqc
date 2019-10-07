version 1.0

# ======================================================
# Workflow accepts two fastq files, with R1 and R2 reads
# ======================================================
workflow fastQC {
input {
        Array[File]+ inputFastqs
}

scatter (f in inputFastqs) {
 call runFastQC { input: inputFastq = f }
}

Array[File] outputReports = select_all(runFastQC.html_report_file)
Array[File] outputZips    = select_all(runFastQC.zip_bundle_file)

meta {
    author: "Peter Ruzanov"
    email: "peter.ruzanov@oicr.on.ca"
    description: "FastQC 3.0"
}

output {
 File html_report_R1  = outputReports[0]
 File zip_bundle_R1   = outputZips[0]
 File? html_report_R2 = if length(outputReports) > 1 then outputReports[1] else ""
 File? zip_bundle_R2  = if length(outputZips) > 1 then outputZips[1] else ""
}

}

# ===================================
#            MAIN STEP
# ===================================
task runFastQC {
input {
        Int?   jobMemory = 6
        File   inputFastq
        String? modules = "perl/5.28 java/8 fastqc/0.11.8"
}

command <<<
 set -euo pipefail
 FASTQC=$(which fastqc)
 JAVA=$(which java)
 perl $FASTQC ~{inputFastq} --java=$JAVA --noextract --outdir "."
>>>

parameter_meta {
 jobMemory: "Memory allocated to fastqc"
 inputFastq: "Input fastq file, gzipped"
 modules: "Names and versions of required modules"
}

runtime {
  memory:  "~{jobMemory} GB"
  modules: "~{modules}"
}

output {
  File? html_report_file = "~{basename(inputFastq, '.fastq.gz')}_fastqc.html"
  File? zip_bundle_file  = "~{basename(inputFastq, '.fastq.gz')}_fastqc.zip"
}
}


