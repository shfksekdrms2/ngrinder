/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ngrinder;

import com.beust.jcommander.*;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
@Parameters(separators = "=")
public class NGrinderControllerStarter {

	@Parameters(separators = "=")
	enum ClusterMode {
		none {
			@Parameter(names = "-controller-port", description = "agent connection port",
					validateValueWith = PortAvailabilityValidator.class)
			public Integer controllerPort = null;

			public void process() {
				if (controllerPort != null) {
					System.setProperty("controller.controller_port", controllerPort.toString());
				}
			}
		},
		easy {
			@Parameter(names = "-cluster-host", required = false,
					description = "This cluster member's cluster communication host. The default value is the " +
							"first non-localhost address. if it's localhost, " +
							"it can only communicate with the other cluster members in the same machine.")
			private String clusterHost = null;

			@Parameter(names = "-cluster-port", required = false,
					description = "This cluster member's cluster communication port. Each cluster should have the " +
							"unique port.",
					validateValueWith = PortAvailabilityValidator.class)
			private Integer clusterPort = null;

			@Parameter(names = "-controller-port", required = true,
					description = "This cluster member's agent connection port",
					validateValueWith = PortAvailabilityValidator.class)
			private Integer controllerPort = null;

			@Parameter(names = "-region", required = true,
					description = "This cluster member's region name")
			private String region = null;


			@Parameter(names = "-database-host", required = false,
					description = "The database host. The default value is localhost")
			private String databaseHost = "localhost";

			@Parameter(names = "-database-port", required = false,
					description = "The H2 database port. The default value is 9092"
			)
			private Integer databasePort = 9092;

			@Parameter(names = "-database-type", required = false,
					description = "The database type. The default value is h2", hidden = true)
			private String databaseType = "h2";

			public void process() {
				System.setProperty("cluster.mode", "easy");
				if (clusterHost != null) {
					System.setProperty("cluster.ip", clusterHost);
				}
				System.setProperty("cluster.port", clusterPort.toString());
				System.setProperty("cluster.region", region);
				System.setProperty("controller.controller_port", controllerPort.toString());
				System.setProperty("database.type", databaseType);
				if ("h2".equals(databaseType)) {
					if (tryConnection(databaseHost, databasePort)) {
						throw new ParameterException("Failed to connect h2 db. Please run the h2 TcpServer in " +
								"advance\nor set the correct -database-host and -database-port parameters");
					}
					System.setProperty("database.url", "tcp://" + this.databaseHost + ":" + databasePort + "/db/ngrinder");
				} else {
					if (tryConnection(databaseHost, databasePort)) {
						throw new ParameterException("Failed to connect cubrid db. Please run the cubrid db " +
								"in advance\nor set the correct -database-host and -database-port parameters");
					}
					System.setProperty("database.url", this.databaseHost + ":" + this.databasePort);
				}
			}

		},
		advanced {
			public void process() {
				System.setProperty("cluster.mode", "advanced");
			}
		};

		public void parseArgs(String[] args) {
			JCommander commander = new JCommander(ClusterMode.this);
			String clusterModeOption = "";
			if (this != ClusterMode.none) {
				clusterModeOption = " -cluster-mode=" + name();
			}
			commander.setProgramName(getRunningCommand() + clusterModeOption);
			try {
				commander.parse(args);
				process();
			} catch (Exception e) {
				System.err.println(e.getMessage());
				commander.usage();
				System.exit(-1);
			}
		}

		abstract void process();
	}

	public static boolean tryConnection(String byConnecting, int port) {
		Socket socket = null;
		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress(byConnecting, port), 2000); // 2 seconds timeout
		} catch (Exception e) {
			return false;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception e) {
					//
				}
			}
		}
		return true;
	}

	class PortRangeValidator implements IValueValidator<Integer> {
		@Override
		public void validate(String name, Integer value) throws ParameterException {
			if (value > Character.MAX_VALUE && value < 0) {
				throw new ParameterException(name + "=" + value + " port is used. The port should be within 0 and " +
						Character.MAX_VALUE);
			}
			InetAddress localHost;
			try {
				localHost = InetAddress.getLocalHost();
			} catch (Exception e) {
				throw new RuntimeException("Failed to get the localhost");
			}
			if (!checkExactPortAvailability(localHost, value)) {
				throw new ParameterException(name + "=" + value + " port is already occupied by the other system " +
						"or failed to bind. Please use the other port");
			}
		}
	}


	class PortAvailabilityValidator extends PortRangeValidator {
		@Override
		public void validate(String name, Integer value) throws ParameterException {
			super.validate(name, value);
			InetAddress localHost;
			try {
				localHost = InetAddress.getLocalHost();
			} catch (Exception e) {
				throw new RuntimeException("Failed to get the localhost");
			}
			if (!checkExactPortAvailability(localHost, value)) {
				throw new ParameterException(name + "=" + value + " port is already occupied by the other system " +
						"or failed to bind. Please use the other port");
			}
		}
	}


	/**
	 * Check if the given port is available.
	 *
	 * @param addr address to be bound
	 * @param port port to be checked
	 * @return true if available
	 */
	public static boolean checkExactPortAvailability(InetAddress inetAddress, int port) {
		ServerSocket socket = null;
		try {
			if (inetAddress == null) {
				socket = new ServerSocket(port);
			} else {
				socket = new ServerSocket(port, 1, inetAddress);
			}
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					// FALL THROUGH
				}
			}
		}
	}

	private static final String NGRINDER_DEFAULT_FOLDER = ".ngrinder";
	@Parameter(names = "-port", description = "HTTP port of the server, The default is 8080")
	private Integer port = 8080;

	@Parameter(names = "-context-path", description = "context path of the embedded web application. The default is /")
	private String contextPath = "/";

	@Parameter(names = "-cluster-mode", description = "nGrinder cluster-mode can be easy or advanced  ")
	private String clusterMode = "none";

	@Parameter(names = "-home", description = "nGrinder home")
	private String home = null;

	@Parameter(names = {"-help", "-?"}, description = "prints this message", hidden = true)
	private Boolean help = false;

	@DynamicParameter(names = "-D", description = "Dynamic parameters")
	private Map<String, String> params = new HashMap<String, String>();


	public static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}

	public static String defaultIfEmpty(String str, String defaultStr) {
		return isEmpty(str) ? defaultStr : str;
	}

	public File resolveHome() {
		String userHomeFromEnv = System.getenv("NGRINDER_HOME");
		String userHomeFromProperty = System.getProperty("ngrinder.home");
		String userHome = defaultIfEmpty(userHomeFromProperty, userHomeFromEnv);
		return (!isEmpty(userHome)) ? new File(userHome) : new File(
				System.getProperty("user.home"), NGRINDER_DEFAULT_FOLDER);
	}


	private void run() {
		Server server = new Server();
		SocketConnector connector = new SocketConnector();
		// Set some timeout options to make debugging easier.
		connector.setMaxIdleTime(1000 * 60 * 60);
		connector.setSoLingerTime(-1);
		connector.setPort(port);
		server.setConnectors(new Connector[]{connector});

		WebAppContext context = new WebAppContext();
		final File home = resolveHome();
		//noinspection ResultOfMethodCallIgnored
		home.mkdirs();
		context.setTempDirectory(home);
		context.setServer(server);
		if (!contextPath.startsWith("/")) {
			contextPath = "/" + contextPath;
		}
		context.setContextPath(contextPath);

		String war = getWarName();
		context.setWar(war);
		server.setHandler(context);
		try {
			server.start();
			//noinspection StatementWithEmptyBody
			while (System.in.read() != 'q') {
				// Fall through
			}
			server.stop();
			server.join();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	private static String getWarName() {
		ProtectionDomain protectionDomain = NGrinderControllerStarter.class.getProtectionDomain();
		String warName = protectionDomain.getCodeSource().getLocation().toExternalForm();
		if (warName.endsWith("/classes/")) {
			warName = "ngrinder-controller-X.X.war";
		}
		return warName;
	}

	private static long getMaxPermGen() {
		for (MemoryPoolMXBean each : ManagementFactory.getMemoryPoolMXBeans()) {
			if (each.getName().endsWith("Perm Gen")) {
				return each.getUsage().getMax();
			}
		}
		return Long.MAX_VALUE;
	}

	public static void main(String[] args) throws Exception {
		if (System.getProperty("unit-test") == null && getMaxPermGen() < (1024 * 1024 * 200)) {
			System.out.println(
					"nGrinder needs quite big perm-gen memory.\n" +
							"Please run nGrinder with the following command.\n" +
							getRunningCommand());
			System.exit(-1);
		}
		NGrinderControllerStarter server = new NGrinderControllerStarter();
		JCommander commander = new JCommander(server);
		commander.setAcceptUnknownOptions(true);
		commander.setProgramName("ngrinder");
		commander.parse(args);

		if (server.help) {
			commander.usage();
			System.exit(0);
		}

		if (server.home != null) {
			System.setProperty("ngrinder.home", server.home);
		}
		final List<String> unknownOptions = commander.getUnknownOptions();
		final ClusterMode clusterMode = ClusterMode.valueOf(server.clusterMode);
		clusterMode.parseArgs(unknownOptions.toArray(new String[unknownOptions.size()]));
		System.getProperties().putAll(server.params);
		server.run();
	}

	private static String getRunningCommand() {
		return "java -XX:MaxPermSize=200m -jar  " + new File(getWarName()).getName();
	}

}