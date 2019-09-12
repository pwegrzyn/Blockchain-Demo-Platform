package blockchain.crypto;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.jocl.CL.*;

import org.jocl.*;

public class Sha256 {

    private static final int SHA256_PLAINTEXT_LENGTH = 64; // 64 Bytes = 512 bits per block
    private static final int SHA256_BINARY_SIZE = 32;
    private static final int SHA256_RESULT_SIZE = 8; // 8 ints = 32 Bytes = 256 bits
    private static final int UINT_SIZE = 4;

    private int lth = 0;

    // The platform, device type and device number that will be used
    private static final long deviceType = CL_DEVICE_TYPE_GPU;
    private static final int deviceIndex = 0;

    private String kernelCode = ""; // Kernel source code
    private cl_command_queue commandQueue;
    private static cl_context context;
    private char[] dataArray;
    private cl_mem dataMem, dataInfo, messageDigest;
    private cl_program program;
    private cl_kernel kernel;
    private long[] global_work_size;
    private long[] local_work_size;
    private int[] datai = new int[3];
    private int[] result;

    private static Sha256 sha256;
    static {
        sha256 = new Sha256();
        try {
            sha256.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println(Sha256.calculateSHA256("abc00"+32402715));
    }

    public static String calculateSHA256(String in) {
        sha256.setData(in);
        String result = sha256.crypt();
        return result;
    }

    private void setData(String input) {
        dataArray = input.toCharArray();
        lth = dataArray.length;
    }

    private void init() throws IOException {
        init(1);
    }

    private void init(int n) throws IOException {

        destroy();

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[numPlatforms - 1];

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

        result = new int[8 * n];
    }

    private String crypt() {
        //initialize data
        dataMem = clCreateBuffer(context, CL_MEM_READ_ONLY, lth * SHA256_PLAINTEXT_LENGTH,
                null, null);
        dataInfo = clCreateBuffer(context, CL_MEM_READ_ONLY, UINT_SIZE * 3, null, null);
        messageDigest = clCreateBuffer(context, CL_MEM_WRITE_ONLY, Sizeof.cl_uint * SHA256_RESULT_SIZE * global_work_size[0], null, null);

        if (dataMem == null || dataInfo == null || messageDigest == null) {
            throw new RuntimeException("System couldn't create non-zero buffer objects");
        }

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(dataInfo));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(dataMem));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(messageDigest));


        datai[0] = SHA256_PLAINTEXT_LENGTH;
        datai[1] = (int) global_work_size[0];
        datai[2] = lth;

        clEnqueueWriteBuffer(commandQueue, dataInfo, CL_TRUE, 0,
                UINT_SIZE * 3, Pointer.to(datai), 0, null, null);

        clEnqueueWriteBuffer(commandQueue, dataMem, CL_TRUE, 0,
                UINT_SIZE * dataArray.length, Pointer.to(dataArray), 0, null, null);

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, messageDigest, CL_TRUE, 0,
                Sizeof.cl_uint * SHA256_RESULT_SIZE, Pointer.to(result), 0, null, null);

        // Release memory objects
        clReleaseMemObject(dataInfo);
        clReleaseMemObject(dataMem);
        clReleaseMemObject(messageDigest);

        return resultToString();
    }

    private void load() throws IOException {
        // Read kernel
        BufferedReader br = new BufferedReader(new FileReader("src/main/java/blockchain/kernel/Sha256.cl"));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while (true) {
            line = br.readLine();
            if (line == null) {
                break;
            }
            sb.append(line + "\n");
        }
        kernelCode = sb.toString();
    }

    private void destroy() {
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

    private String resultToString() {
        StringBuilder stringBld = new StringBuilder();
        for (int i = 0; i < SHA256_RESULT_SIZE; i++) {
            stringBld.append(String.format("%08x", result[i]));
        }
        return stringBld.toString();
    }
}
