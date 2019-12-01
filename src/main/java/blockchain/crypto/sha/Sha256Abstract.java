package blockchain.crypto.sha;

import org.jocl.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.jocl.CL.*;
import static org.jocl.CL.clReleaseContext;

public abstract class Sha256Abstract {

    protected static final int SHA256_PLAINTEXT_LENGTH = 64; // 64 Bytes = 512 bits per block
    protected static final int SHA256_RESULT_SIZE = 8; // 8 ints = 32 Bytes = 256 bits
    protected static final int UINT_SIZE = 4;


    // The platform, device type and device number that will be used
    protected static final long deviceType = CL_DEVICE_TYPE_GPU;
    protected static final int deviceIndex = 0;
    protected static final int platformIndex = 0;
    protected String kernelPath = "";

    protected String kernelCode = ""; // Kernel source code
    protected cl_command_queue commandQueue;
    protected cl_context context;
    protected char[] dataArray;
    protected cl_mem dataMem, dataInfo;
    protected cl_program program;
    protected cl_kernel kernel;
    protected long[] global_work_size;
    protected long[] local_work_size;
    protected int[] datai = new int[3];
    protected int lth = 0;

    protected void init() throws IOException {
        init(1);
    }

    protected void init(int n) throws IOException {
        destroy();

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue, with profiling info enabled
        cl_queue_properties properties = new cl_queue_properties();
        properties.addProperty(CL_QUEUE_PROPERTIES, CL_QUEUE_PROFILING_ENABLE);
        commandQueue = clCreateCommandQueueWithProperties(
                context, device, properties, null);

        // Load kernel code
        load();

        // Create the program from the source code
        program = clCreateProgramWithSource(context,
                1, new String[]{kernelCode}, null, null);
        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        kernel = clCreateKernel(program, "sha256Kernel", null);

        // Set the work-item dimensions
        global_work_size = new long[]{n};
        local_work_size = new long[]{1};
    }

    protected void load() {
        // Read kernel
        BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(kernelPath)));
        kernelCode = br.lines().collect(Collectors.joining("\n"));
    }

    protected void destroy() {
        // Release previous kernel, program
        if (kernel != null) {
            clReleaseKernel(kernel);
        }
        if (program != null) {
            clReleaseProgram(program);
        }
        if (commandQueue != null) {
            clReleaseCommandQueue(commandQueue);
        }
        if (context != null) {
            clReleaseContext(context);
        }
    }

    protected String resultToString(int[] resultInts) {
        StringBuilder stringBld = new StringBuilder();
        for (int i = 0; i < SHA256_RESULT_SIZE; i++) {
            stringBld.append(String.format("%08x", resultInts[i]));
        }
        return stringBld.toString();
    }
}
