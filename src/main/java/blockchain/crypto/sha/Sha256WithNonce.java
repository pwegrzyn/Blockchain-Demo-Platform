package blockchain.crypto.sha;

import org.jocl.*;

import static org.jocl.CL.*;

public class Sha256WithNonce extends Sha256Abstract {
    private cl_mem nonce, messageDigest;
    private int[] nonceInt = new int[1];
    private int[] result = new int[8];
    ;

    public Sha256WithNonce() {
        kernelPath = "kernel/Sha256WithNonce.cl";
        try {
            init();
        } catch (Exception e) {
            System.err.println("Error while initializing Sha256WithNonce instance");
            e.printStackTrace();
        }
    }

    public void setData(String input, int nonce) {
        result = new int[8 * (int) global_work_size[0]];
        dataArray = input.toCharArray();
        lth = dataArray.length;
        nonceInt[0] = nonce;
    }

    public String crypt() {
        //initialize data
        dataMem = clCreateBuffer(context, CL_MEM_READ_ONLY, lth * SHA256_PLAINTEXT_LENGTH,
                null, null);
        dataInfo = clCreateBuffer(context, CL_MEM_READ_ONLY, UINT_SIZE * 3, null, null);
        messageDigest = clCreateBuffer(context, CL_MEM_WRITE_ONLY, Sizeof.cl_uint * SHA256_RESULT_SIZE * global_work_size[0], null, null);
        nonce = clCreateBuffer(context, CL_MEM_READ_ONLY, Sizeof.cl_uint, null, null);

        if (dataMem == null || dataInfo == null || messageDigest == null || nonce == null) {
            throw new RuntimeException("System couldn't create non-zero buffer objects");
        }

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(dataInfo));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(dataMem));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(messageDigest));
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(nonce));

        datai[0] = SHA256_PLAINTEXT_LENGTH;
        datai[1] = (int) global_work_size[0];
        datai[2] = lth;

        clEnqueueWriteBuffer(commandQueue, dataInfo, CL_TRUE, 0,
                UINT_SIZE * 3, Pointer.to(datai), 0, null, null);

        clEnqueueWriteBuffer(commandQueue, dataMem, CL_TRUE, 0,
                UINT_SIZE * dataArray.length, Pointer.to(dataArray), 0, null, null);

        clEnqueueWriteBuffer(commandQueue, nonce, CL_TRUE, 0,
                UINT_SIZE, Pointer.to(nonceInt), 0, null, null);

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
        clReleaseMemObject(nonce);

        return resultToString(result);
    }
}
