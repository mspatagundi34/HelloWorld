import groovy.xml.*
import groovy.io.FileType
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

println "=====Start Updating pom.xml====="

// Load the pom.xml
def pomFile = new File("pom.xml")
def xml = new XmlSlurper(false, false).parse(pomFile)

// update artifact version
//def version = (xml.version).split(".") 
println "version $xml.version"

//Read config file
def configData = new JsonSlurper().parseText(new File(".github/workflows/Config.json").text)

xml.properties['app.runtime'] = configData["app.runtime"]
xml.properties['mule.maven.plugin.version'] = configData["mule.maven.plugin.version"]

// Update maven compiler plugin 
xml.build.plugins.plugin.each {
    plugin -> if (plugin.groupId == 'org.apache.maven.plugins' && plugin.artifactId == "maven-clean-plugin") {
        plugin.version = "3.2.0"
        plugin.configuration.source = "17"
        plugin.configuration.target = "17"
        println "Updated '$plugin.groupId'"
    }
    // Note: We may need to remove this
    if (plugin.groupId == 'org.apache.maven.plugins' && plugin.artifactId == "maven-compiler-plugin") {
        plugin.version = "3.9.4"
        plugin.configuration.source = "17"
        plugin.configuration.target = "17"
        println "Updated '$plugin.groupId'"
    }
}

configData.dependencies.each {
    conf -> xml.dependencies.dependency.each {
        dependency -> if (dependency.groupId == conf.groupId && dependency.artifactId == conf.artifactId) {
            dependency.version = conf.version
            println "Updated artifact '$dependency.artifactId'  to '$conf.version'"
        }
    }
}


// Update exchange repo url
xml.repositories.repository.each {
    repo -> if (repo.id == "anypoint-exchange-v2") {
        repo.id = "anypoint-exchange-v3"
        repo.url = "https://maven.anypoint.mulesoft.com/api/v3/maven"
    }
}
    

// Check if the depdendency node exists
def targetNode1 = xml.dependencies.'*'.find {
    it.artifactId == 'mule-db-connector'
}

def targetNode2 = xml.dependencies.'*'.find {
    it.groupId == 'javax.xml.bind'
}
// Check if the plugin node exists
def targetNode3 = xml.pluginRepositories.'*'.find {
    it.id == 'synergian-repo'
}

def targetNode4 = xml.pluginRepositories.'*'.find {
    it.artifactId == 'mule-objectstore-connector'
}

// Append the new dependency node only if the target node is not present
if (targetNode1) {
    println "DataBase depdendency present"
    // Add new dependency because for java 17 xml bind dependency needed to mitigate serialization error
    if (!targetNode2) {
        println "XML Bind depdendency not present"
        xml.dependencies.appendNode {
            dependency {
                groupId 'javax.xml.bind'
                artifactId 'jaxb-api'
                version '2.3.1'
                //type 'zip'
                //scope 'provided'
            }
        }
    }
}

// Append the new pluginRepository node only if the target node is not present
if (!targetNode3) {
    println "PluginRepository Node Not present"
    // Add new PluginRepository
    xml.pluginRepositories.appendNode {
        pluginRepository {
            id 'synergian-repo'
            url 'https://raw.github.com/synergian/wagon-git/releases'
            snapshots {
                enabled true
            }
        }
    }
}

// Append the new pluginRepository node only if the target node is not present
if (!targetNode4) {
    println "Add objectstore dependency because latest studio sometime doesn't include java 17 compatiable objectstore as built in connector"
    // Add new PluginRepository
    xml.dependencies.appendNode {
        dependency {
            groupId 'org.mule.connectors'
            artifactId 'mule-objectstore-connector'
            version '1.2.2'
            classifier 'mule-plugin'
        }
    }
}
// Remove a specific dependency
def dependencyToRemove = xml.dependencies.find {
    dependency -> dependency.groupId == "com.mulesoft.modules" && dependency.artifactId 
        == "mule-latency-connector"
}
	
if (dependencyToRemove) {
    println("Removing mule-latency-connector dependency...")
    xml.dependencies.remove(dependencyToRemove)
    println("mule-latency-connector dependency removed.")
} else {
    println("mule-latency-connector dependency not found.")
}

XmlUtil.serialize(xml, new PrintWriter(new File("pom.xml")))

println "=====Finished Updating pom.xml====="
//-----------------------------------------------------------------------------------//

println "=====Start Updating mule-artifact.json====="
// Load the artifact.json file
def jsonData = new JsonSlurper().parseText(new File("mule-artifact.json").text)

jsonData.minMuleVersion = configData["app.runtime"]
jsonData.javaSpecificationVersions = configData["javaSpecificationVersions"]

println "Min Mule Version = ${jsonData.minMuleVersion}"

new File("mule-artifact.json").write(new JsonBuilder(jsonData).toPrettyString())

println "Finished Updating mule-artifact.json"

//-----------------------------------------------------------------------------------//

println "=====Start Updating mule config xml files====="
def folderPath = "src/main/mule" // Replace with the actual path

def folder = new File(folderPath)

folder.eachFile(FileType.FILES) {
    File file -> if (file.name.toLowerCase().endsWith(".xml")) {
        def isFileUpdated = false
        //def configXml = new XmlSlurper().parse(file)
        def configXml = file.getText()
        configData.replaceData.each {
            str -> if (configXml.contains(str.oldValue)) {
                println "String '$str.oldValue' found in the file."
                def modifiedContent = configXml.replaceAll(str.oldValue, str.newValue)
                isFileUpdated = true
                configXml = modifiedContent
                //println "$modifiedContent"
                //XmlUtil.serialize(modifiedContent, new PrintWriter(file))
            } else {
                println "String '$str.oldValue' not found in the file '$file'"
            }
        }
        if (isFileUpdated)
        {
            //XmlUtil.serialize(configXml, new PrintWriter(file))
		//file.write(XmlUtil.serialize(configXml))
		def stringWriter = new StringWriter()
def node = new XmlParser().parseText(configXml);
new XmlNodePrinter(new PrintWriter(stringWriter)).print(node)

println stringWriter.toString()
		file.write(XmlUtil.serialize(new XmlNodePrinter(new PrintWriter(stringWriter)).print(node)))
	}// Find the element containing the search string
    }
}
println "=====Finished Updating mule config xml files====="
