package blockchain.crypto.sha;

import org.jocl.*;

import java.nio.ByteBuffer;

import static org.jocl.CL.*;

public class Sha256NonceSearching extends Sha256Abstract {
    protected cl_mem nounceOffset, targetHash, foundFlags;
    private int[] targetHashInts = new int[8];
    private int[] offset = new int[1];
    private int[] foundFlagsInts;
    private long result;

    public Sha256NonceSearching(int workSize) {
        kernelPath = "kernel/Sha256NonceSearching.cl";
        try {
            init(workSize);
        } catch (Exception e) {
            System.err.println("Error while initializing Sha256NonceSearching instance");
            e.printStackTrace();
        }
    }

    public void setData(String input, String targetHash) {
        dataArray = input.toCharArray();
        lth = dataArray.length;
        offset[0] = 0;
        for (int i = 0, j = 0; i < SHA256_PLAINTEXT_LENGTH; i += SHA256_RESULT_SIZE, j++) {
            targetHashInts[j] = getDecimalFromHex(targetHash.substring(i, i + SHA256_RESULT_SIZE));
        }
    }

    public int crypt() {
        //initialize data
        dataMem = clCreateBuffer(context, CL_MEM_READ_ONLY, lth * SHA256_PLAINTEXT_LENGTH,
                null, null);
        dataInfo = clCreateBuffer(context, CL_MEM_READ_ONLY, UINT_SIZE * 3, null, null);
        nounceOffset = clCreateBuffer(context, CL_MEM_READ_ONLY, Sizeof.cl_uint, null, null);
        targetHash = clCreateBuffer(context, CL_MEM_READ_ONLY, Sizeof.cl_uint * SHA256_RESULT_SIZE, null, null);
        foundFlags = clCreateBuffer(context, CL_MEM_WRITE_ONLY, Sizeof.cl_uint * global_work_size[0], null, null);

        if (dataMem == null || dataInfo == null || nounceOffset == null || targetHash == null || foundFlags == null) {
            throw new RuntimeException("System couldn't create non-zero buffer objects");
        }

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(dataInfo));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(dataMem));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(nounceOffset));
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(targetHash));
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(foundFlags));

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

            // Read the output nonce
            clEnqueueReadBuffer(commandQueue, foundFlags, CL_TRUE, 0,
                    Sizeof.cl_uint * global_work_size[0], Pointer.to(foundFlagsInts), 0, null, null);

            for (int i = 0; i < global_work_size[0]; i++) {
                long value = getUnsignedInt(foundFlagsInts[i]);
                if (value > 0)
                    result = value;
            }

            if (result < 0)
                offset[0] += global_work_size[0];
            if (offset[0] + global_work_size[0] < 0 || offset[0] > 1000000) {
                break;
            }
        } while (result < 0);

        // Release memory objects
        clReleaseMemObject(dataInfo);
        clReleaseMemObject(dataMem);
        clReleaseMemObject(nounceOffset);
        clReleaseMemObject(targetHash);
        clReleaseMemObject(foundFlags);

        return (int) result;
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
