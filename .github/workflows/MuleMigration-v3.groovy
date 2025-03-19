import groovy.xml.*
import groovy.io.FileType
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

println "=====Start Updating pom.xml====="

// Load the pom.xml
def pomFile = new File("pom.xml")
def xml = new XmlSlurper( false, false ).parse(pomFile)

// update artifact version
//def version = (xml.version).split(".") 
 println "version $xml.version"

//Read config file
def configData = new JsonSlurper().parseText(new File(".github/workflows/Config.json").text)

xml.properties['app.runtime'] = configData["app.runtime"]
xml.properties['mule.maven.plugin.version'] = configData["mule.maven.plugin.version"]

// Update maven compiler plugin 
xml.build.plugins.plugin.each{ plugin ->
    if(plugin.groupId == 'org.apache.maven.plugins' && plugin.artifactId == "maven-clean-plugin"){
        plugin.version = "3.2.0"
	plugin.configuration.source = "17"
	plugin.configuration.target = "17"
        println "Updated $plugin.groupId"
   }

	// Note: We may need to remove this
	if(plugin.groupId == 'org.apache.maven.plugins' && plugin.artifactId == "maven-compiler-plugin"){
	        plugin.version = "3.9.4"
		plugin.configuration.source = "17"
		plugin.configuration.target = "17"
	        println "Updated $plugin.groupId"
   }
}

configData.dependencies.each{ conf ->
    xml.dependencies.dependency.each{ dependency ->
    if(dependency.groupId == conf.groupId && dependency.artifactId == conf.artifactId){
        dependency.version = conf.version
        println "Updated artifact $dependency.artifactId  to $conf.version"
    }
}
    }


// Update exchange repo url
    xml.repositories.repository.each{ repo ->
    if(repo.id == "anypoint-exchange-v2"){
        repo.id = "anypoint-exchange-v3"
	repo.url = "https://maven.anypoint.mulesoft.com/api/v3/maven"
    }
}
    

// Check if the depdendency node exists
def targetNode1 = xml.dependencies.'*'.find{ it.artifactId == 'mule-db-connector' }

def targetNode2 = xml.dependencies.'*'.find{ it.groupId == 'javax.xml.bind' }
// Check if the plugin node exists
def targetNode3 = xml.pluginRepositories.'*'.find{ it.id == 'synergian-repo' }

// Append the new dependency node only if the target node is not present
if(targetNode1){
   println "DataBase depdendency present"
 // Add new dependency
if(!targetNode2){
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
if(!targetNode3){
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
	// Remove a specific dependency
	def dependencyToRemove = xml.dependencies.find { dependency ->
	    dependency.groupId == "com.mulesoft.modules" && dependency.artifactId == "mule-latency-connector"
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
//def searchString = "error.muleMessage" // Replace with the string to search for
def searchString ="error.errorType.parentErrorType.asString"
//def newValue = "error.errorMessage" // Replace with the new value
def newValue = "error.errorType.namespace ++ ':' ++ error.errorType.identifier"

def folder = new File(folderPath)

folder.eachFile(FileType.FILES) { File file ->
    if (file.name.toLowerCase().endsWith(".xml")) {
        //def configXml = new XmlSlurper().parse(file)
	    def configXml = file.getText()
        // Find the element containing the search string
	    if (configXml.contains(searchString)) {
		    
    println "String '$searchString' found in the file."
def modifiedContent = configXml.replaceAll(searchString, newValue)
 //println "$modifiedContent"
XmlUtil.serialize(modifiedContent, new PrintWriter(file))
} else {
    println "String '$searchString' not found in the file."
}      
    }
}
println "=====Finished Updating mule config xml files====="
