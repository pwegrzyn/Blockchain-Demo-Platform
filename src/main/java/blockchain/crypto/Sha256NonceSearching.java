package blockchain.crypto;

import org.jocl.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.jocl.CL.*;

public class Sha256NonceSearching {

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
    private cl_mem dataMem, dataInfo, messageDigest, nounceOffset, targetHash, foundFlags;
    private cl_program program;
    private cl_kernel kernel;
    private long result;
    private long[] global_work_size;
    private long[] local_work_size;
    private int[] datai = new int[3];
    private int[] targetHashInts = new int[8];
    private int[] offset = new int[1];
    private int[] foundFlagsInts;

    private static Sha256NonceSearching sha256;

    static {
        sha256 = new Sha256NonceSearching();
        try {
            sha256.init(1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 1000; i++) {
            System.out.println("\nResult is: " + Sha256NonceSearching.calculateSHA256("abc", "000000000f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"));
        }
    }

    public static int calculateSHA256(String in, String targetHash) {
        sha256.setData(in, targetHash);
        int res = sha256.crypt();
        return res;
    }

    private void setData(String input, String targetHash) {
        dataArray = input.toCharArray();
        lth = dataArray.length;
        offset[0] = 0;
        for (int i = 0, j = 0; i < SHA256_PLAINTEXT_LENGTH; i += SHA256_RESULT_SIZE, j++) {
            targetHashInts[j] = getDecimalFromHex(targetHash.substring(i, i + SHA256_RESULT_SIZE));
        }
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
        CL.setExceptionsEnabled(true);
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        kernel = clCreateKernel(program, "sha256Kernel", null);

        // Set the work-item dimensions
        global_work_size = new long[]{n};
        local_work_size = new long[]{1};
    }

    private int crypt() {
        //initialize data
        dataMem = clCreateBuffer(context, CL_MEM_READ_ONLY, lth * SHA256_PLAINTEXT_LENGTH,
                null, null);
        dataInfo = clCreateBuffer(context, CL_MEM_READ_ONLY, UINT_SIZE * 3, null, null);
        messageDigest = clCreateBuffer(context, CL_MEM_WRITE_ONLY, Sizeof.cl_uint * SHA256_RESULT_SIZE * global_work_size[0], null, null);
        nounceOffset = clCreateBuffer(context, CL_MEM_READ_ONLY, Sizeof.cl_uint, null, null);
        targetHash = clCreateBuffer(context, CL_MEM_READ_ONLY, Sizeof.cl_uint * SHA256_RESULT_SIZE, null, null);
        foundFlags = clCreateBuffer(context, CL_MEM_WRITE_ONLY, Sizeof.cl_uint * global_work_size[0], null, null);

        if (dataMem == null || dataInfo == null || messageDigest == null || nounceOffset == null || targetHash == null || foundFlags == null) {
            throw new RuntimeException("System couldn't create non-zero buffer objects");
        }

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(dataInfo));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(dataMem));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(messageDigest));
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(nounceOffset));
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(targetHash));
        clSetKernelArg(kernel, 5, Sizeof.cl_mem, Pointer.to(foundFlags));

        datai[0] = SHA256_PLAINTEXT_LENGTH;
        datai[1] = (int) global_work_size[0];
        datai[2] = lth;
        result = -1;

        foundFlagsInts = new int[(int) global_work_size[0]];
        for (int i = 0; i < foundFlagsInts.length; i++) {
            foundFlagsInts[i] = 0;
        }

        do {
            clEnqueueWriteBuffer(commandQueue, dataInfo, CL_TRUE, 0,
                    UINT_SIZE * 3, Pointer.to(datai), 0, null, null);

            clEnqueueWriteBuffer(commandQueue, dataMem, CL_TRUE, 0,
                    UINT_SIZE * dataArray.length, Pointer.to(dataArray), 0, null, null);

            clEnqueueWriteBuffer(commandQueue, nounceOffset, CL_TRUE, 0,
                    UINT_SIZE, Pointer.to(offset), 0, null, null);

            clEnqueueWriteBuffer(commandQueue, targetHash, CL_TRUE, 0,
                    UINT_SIZE * SHA256_RESULT_SIZE, Pointer.to(targetHashInts), 0, null, null);

            clEnqueueWriteBuffer(commandQueue, foundFlags, CL_TRUE, 0,
                    UINT_SIZE * foundFlagsInts.length, Pointer.to(foundFlagsInts), 0, null, null);

            // Execute the kernel
            clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                    global_work_size, local_work_size, 0, null, null);

            clEnqueueReadBuffer(commandQueue, foundFlags, CL_TRUE, 0,
                    Sizeof.cl_uint * global_work_size[0], Pointer.to(foundFlagsInts), 0, null, null);

            for (int i = 0; i < global_work_size[0]; i++) {
                long value = getUnsignedInt(foundFlagsInts[i]);
                if (value > 0)
                    result = value;
            }

            if (result < 0)
                offset[0] += global_work_size[0];
            if (offset[0] + global_work_size[0] < 0) {
                break;
            }
        } while (result < 0);

        // Release memory objects
        clReleaseMemObject(dataInfo);
        clReleaseMemObject(dataMem);
        clReleaseMemObject(messageDigest);
        clReleaseMemObject(nounceOffset);
        clReleaseMemObject(targetHash);
        clReleaseMemObject(foundFlags);

        return (int) result;
    }

    private void load() throws IOException {
        // Read kernel
        BufferedReader br = new BufferedReader(new FileReader("src/main/java/blockchain/kernel/Sha256NonceSearching.cl"));
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

    private static int getDecimalFromHex(String hex) {
        String digits = "0123456789ABCDEF";
        hex = hex.toUpperCase();
        int val = 0;
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            int d = digits.indexOf(c);
            val = 16 * val + d;
        }
        return val;
    }

    private static long getUnsignedInt(int x) {
        Integer BITS_PER_BYTE = 8;
        ByteBuffer buf = ByteBuffer.allocate(Long.SIZE / BITS_PER_BYTE);
        buf.putInt(Integer.SIZE / BITS_PER_BYTE, x);
        return buf.getLong(0);
    }
}
