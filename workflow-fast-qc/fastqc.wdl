version 1.0

# ======================================================
# Workflow accepts two fastq files, with R1 and R2 reads
# ======================================================
workflow fastQC {
input {
        Array[File]+ inputFastqs
}

call runFastQC as firstMateFastQC { input: inputFastq = inputFastqs[0] }
if (length(inputFastqs) > 1) {
 call runFastQC as secondMateFastQC { input: inputFastq = inputFastqs[1] }
}

meta {
    author: "Peter Ruzanov"
    email: "peter.ruzanov@oicr.on.ca"
    description: "FastQC 3.0"
}

output {
 File? html_report_R1  = firstMateFastQC.html_report_file
 File? zip_bundle_R1   = firstMateFastQC.zip_bundle_file
 File? html_report_R2 = secondMateFastQC.html_report_file
 File? zip_bundle_R2  = secondMateFastQC.zip_bundle_file
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


