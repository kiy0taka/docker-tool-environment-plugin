package org.jenkinsci.plugins.dockerenv;

import java.io.File;
import java.io.IOException;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerEndpoint;
import org.jenkinsci.plugins.docker.commons.credentials.KeyMaterial;
import org.jenkinsci.plugins.docker.commons.tools.DockerTool;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @author Kiyotaka Oku
 */
public class DockerEnvWrapper extends BuildWrapper {

    private DockerServerEndpoint server;

    private String dockerInstallation;

    @DataBoundConstructor
    public DockerEnvWrapper() {
        this.server = new DockerServerEndpoint(null, null);
    }

    public DockerServerEndpoint getServer() {
        return server;
    }

    @DataBoundSetter
    public void setServer(DockerServerEndpoint server) {
        this.server = server;
    }

    public String getDockerInstallation() {
        return dockerInstallation;
    }

    @DataBoundSetter
    public void setDockerInstallation(String dockerInstallation) {
        this.dockerInstallation = dockerInstallation;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        EnvVars dockerEnv = new EnvVars();

        final KeyMaterial serverKey = server == null ? null : server.newKeyMaterialFactory(build).materialize();
        if (serverKey != null) {
            dockerEnv.putAll(serverKey.env());
        }

        if (StringUtils.isNotEmpty(dockerInstallation)) {
            String executable = DockerTool.getExecutable(dockerInstallation, Computer.currentComputer().getNode(), listener, build.getEnvironment(listener));
            File dockerHome = new File(executable).getParentFile();
            dockerEnv.put("PATH+DOCKER", dockerHome.getAbsolutePath());
        }

        build.getEnvironments().add(hudson.model.Environment.create(dockerEnv));

        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                if (serverKey != null) {
                    serverKey.close();
                }
                return super.tearDown(build, listener);
            }
        };
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Docker tool environment";
        }
    }
}
