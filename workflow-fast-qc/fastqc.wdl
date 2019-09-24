version 1.0

# ======================================================
# Workflow accepts two fastq files, with R1 and R2 reads
# ======================================================
workflow fastQC {
input {
        String? modules = "perl/5.28 java/8 fastqc/0.11.8"
        Array[File] inputFastqs
}


call runFastQC as FastQC_R1 { input: inputFastq = inputFastqs[0], modules = modules }
call runFastQC as FastQC_R2 { input: inputFastq = inputFastqs[1], modules = modules }

output {
  File html_report_file_R1 = FastQC_R1.html_report_file
  File zip_bundle_file_R1  = FastQC_R1.zip_bundle_file
  File html_report_file_R2 = FastQC_R2.html_report_file
  File zip_bundle_file_R2  = FastQC_R2.zip_bundle_file
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

runtime {
  memory:  "~{jobMemory} GB"
  modules: "~{modules}"
}

output {
  File html_report_file = "~{basename(inputFastq, '.fastq.gz')}_fastqc.html"
  File zip_bundle_file  = "~{basename(inputFastq, '.fastq.gz')}_fastqc.zip"
}
}


