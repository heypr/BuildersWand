package dev.heypr.buildersWand;

import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("UnstableApiUsage")
public class LibLoader implements PluginLoader {

    static boolean invuiSupported = true;

    @Override
    public void classloader(@NonNull PluginClasspathBuilder classpathBuilder) {
        String revision = getRevision();
        if (revision != null) {
            MavenLibraryResolver invuiResolver = new MavenLibraryResolver();
            invuiResolver.addRepository(new RemoteRepository.Builder("xenondevs", "default", "https://repo.xenondevs.xyz/releases").build());
            invuiResolver.addRepository(new RemoteRepository.Builder("central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());
            invuiResolver.addDependency(new Dependency(new DefaultArtifact("xyz.xenondevs.invui:inventory-access-" + revision + ":1.49"), null));
            classpathBuilder.addLibrary(invuiResolver);
        }
        else {
            invuiSupported = false;
        }
        MavenLibraryResolver hikariResolver = new MavenLibraryResolver();
        hikariResolver.addRepository(new RemoteRepository.Builder("central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());
        hikariResolver.addDependency(new Dependency(new DefaultArtifact("com.zaxxer:HikariCP:7.0.2"), null));
        classpathBuilder.addLibrary(hikariResolver);
    }

    private static String getRevision() {
        String version = ServerBuildInfo.buildInfo().minecraftVersionId();
        return switch (version) {
            case "1.21.10", "1.21.11" -> "r26";
            case "1.21.7", "1.21.8", "1.21.9" -> "r25";
            case "1.21.6" -> "r24";
            case "1.21.5" -> "r23";
            case "1.21.4" -> "r22";
            case "1.21.2", "1.21.3" -> "r21";
            case "1.21", "1.21.1" -> "r20";
            default -> null;
        };
    }
}
