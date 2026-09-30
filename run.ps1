# Run the Task Planner application
# Usage: .\run.ps1
# Requires: PostgreSQL running with database task_planner_db

$JAVA_HOME = "C:\Users\User\.jdks\ms-21.0.12.1"
$MVN       = "C:\Users\User\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd"

$env:JAVA_HOME = $JAVA_HOME

Write-Host "▶  Starting Task Planner on http://localhost:8080 ..." -ForegroundColor Cyan
& $MVN spring-boot:run -Dspring-boot.run.profiles=local
