package org.example.loan.workflow.deployment

import io.camunda.zeebe.client.ZeebeClient
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component

@Component
class ProcessDeployer(
    private val zeebeClient: ZeebeClient,
    private val resourceLoader: ResourceLoader
) : ApplicationListener<ApplicationReadyEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        deployProcesses()
    }

    private fun deployProcesses() {
        val bpmnFiles = listOf(
            "classpath:bpmn/loan-v1.bpmn",
            "classpath:bpmn/loan-v2.bpmn"
        )

        bpmnFiles.forEach { resourcePath ->
            try {
                val resource = resourceLoader.getResource(resourcePath)
                if (resource.exists()) {
                    val deployment = zeebeClient.newDeployResourceCommand()
                        .addResourceStream(resource.inputStream, resource.filename ?: "process.bpmn")
                        .send()
                        .join()

                    deployment.processes.forEach { process ->
                        logger.info(
                            "Deployed process: bpmnProcessId={}, version={}, processDefinitionKey={}",
                            process.bpmnProcessId,
                            process.version,
                            process.processDefinitionKey
                        )
                    }
                } else {
                    logger.warn("BPMN resource not found: {}", resourcePath)
                }
            } catch (e: Exception) {
                logger.error("Failed to deploy process from {}: {}", resourcePath, e.message, e)
            }
        }
    }
}
