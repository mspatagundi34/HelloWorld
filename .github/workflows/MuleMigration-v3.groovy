import groovy.xml.*
import groovy.io.FileType
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

println "=====Start Updating pom.xml====="

// Load the pom.xml
def pomFile = new File("pom.xml")
def pomxml = new XmlSlurper(false, false).parse(pomFile)

// update artifact version
//def version = (xml.version).split(".") 
println "version $pomxml.version"

//Read config file
def configData = new JsonSlurper().parseText(new File(".github/workflows/Config.json").text)

pomxml.properties['app.runtime'] = configData.properties["app.runtime"]
pomxml.properties['mule.maven.plugin.version'] = configData.properties["mule.maven.plugin.version"]

// Update maven compiler plugin 
pomxml.build.plugins.plugin.each {
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

configData.dependenciesToUpdate.each {
    conf -> pomxml.dependencies.dependency.each {
        dependency -> if (dependency.groupId == conf.groupId && dependency.artifactId == conf.artifactId) {
            dependency.version = conf.version
            println "Updated artifact '$dependency.artifactId' version to '$conf.version'"
        }
    }
}


// Update exchange repo url
pomxml.repositories.repository.each {
    repo -> if (repo.id == "anypoint-exchange-v2") {
        repo.id = "anypoint-exchange-v3"
        repo.url = "https://maven.anypoint.mulesoft.com/api/v3/maven"

	println "Updated respository '$repo.id'"
    }
}
    

// Check if the depdendency node exists
def targetNode1 = pomxml.dependencies.'*'.find {
    it.artifactId == 'mule-db-connector'
}

def targetNode2 = pomxml.dependencies.'*'.find {
    it.groupId == 'javax.xml.bind'
}
// Check if the plugin node exists
def targetNode3 = pomxml.pluginRepositories.'*'.find {
    it.id == 'synergian-repo'
}

def targetNode4 = pomxml.pluginRepositories.'*'.find {
    it.artifactId == 'mule-objectstore-connector'
}

// Append the new dependency node only if the target node is not present
if (targetNode1) {
    println "DataBase dependency present"
    // Add new dependency because for java 17 xml bind dependency needed to mitigate serialization error
    if (!targetNode2) {
        println "javax.xml.bind dependency not present, it's required to mitigate serialization error, adding this dependency"
        pomxml.dependencies.appendNode {
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
    // Add new PluginRepository
    pomxml.pluginRepositories.appendNode {
        pluginRepository {
            id 'synergian-repo'
            url 'https://raw.github.com/synergian/wagon-git/releases'
            snapshots {
                enabled true
            }
	println "Added new plugin repository"
        }
    }
}

// Append the new pluginRepository node only if the target node is not present
if (!targetNode4) {
    println "Add objectstore dependency because latest studio sometime doesn't include java 17 compatiable objectstore as built in connector"
    // Add new PluginRepository
    pomxml.dependencies.appendNode {
        dependency {
            groupId 'org.mule.connectors'
            artifactId 'mule-objectstore-connector'
            version '1.2.2'
            classifier 'mule-plugin'
        }
    }
}
// Remove a specific dependency
/*configData.dependenciesToRemove.each {
    conf -> pomxml.dependencies.dependency.each {
        dependency -> if (dependency.groupId == conf.groupId && dependency.artifactId == conf.artifactId) {
             pomxml.dependencies.remove(true)
    	   println("removed '$conf.artifactId' dependency")
        }
    }
}*/
configData.dependenciesToRemove.each {
    conf ->
def dependencyToRemove = pomxml.dependencies.'*'.find { it.groupId == conf.groupId && it.artifactId 
        == conf.artifactId
}
	if (dependencyToRemove) {
    println("Removing mule-latency-connector dependency...")
    xml.dependencies.remove(dependencyToRemove)
    println("mule-latency-connector dependency removed.")
} else {
    println("remove '$dependencyToRemove'")
}
}
XmlUtil.serialize(pomxml, new PrintWriter(new File("pom.xml")))

println "=====Finished Updating pom.xml====="
//-----------------------------------------------------------------------------------//

println "=====Start Updating mule-artifact.json====="
// Load the artifact.json file
def jsonData = new JsonSlurper().parseText(new File("mule-artifact.json").text)

jsonData.minMuleVersion = configData.properties["app.runtime"]
	println "Update minMuleVersion to  ${jsonData.minMuleVersion} in mule-artifact.json"

jsonData.javaSpecificationVersions = configData.properties["javaSpecificationVersions"]
	println "Added/Updated javaSpecificationVersions to  ${jsonData.javaSpecificationVersions} in mule-artifact.json"

new File("mule-artifact.json").write(new JsonBuilder(jsonData).toPrettyString())

println "Finished Updating mule-artifact.json"

//-----------------------------------------------------------------------------------//

println "=====Start Updating mule config xml files====="
def folderPath = "src/main/" // Replace with the actual path

def folder = new File(folderPath)

folder.eachFileRecurse(FileType.FILES) {
    File file -> 
	def fileName = file.name.toLowerCase()
	if (fileName.endsWith(".xml") || fileName.endsWith(".dwl")) {

	// Define var to identify whether file is modified or not
        def isFileModified = false
        //def fileData = new XmlSlurper().parse(file)
        def fileContent = file.getText()
        configData.replaceData.each {
            str -> if (fileContent.contains(str.oldValue)) {
                def modifiedContent = fileContent.replaceAll(str.oldValue, str.newValue)
		println "String '$str.oldValue' found and replaced with '$str.newValue' in the file '$file'"
		    
                isFileModified = true // set true since file is modified
                fileContent = modifiedContent
                //println "$modifiedContent"
                //XmlUtil.serialize(modifiedContent, new PrintWriter(file))
            } 
		/*else {
                println "String '$str.oldValue' not found in the file '$file'"
            } */
        }
        if (isFileModified)
        {
		 println "Writring modified file '$file'"
		if(fileName.endsWith(".xml"))
		{
		file.write(XmlUtil.serialize(new XmlSlurper(false, false, true).parseText(fileContent)))
		}
		else if(fileName.endsWith(".dwl"))
		{
			file.write(fileContent)
		}
	}// Find the element containing the search string
    }
}
println "=====Finished Updating mule config xml files====="
