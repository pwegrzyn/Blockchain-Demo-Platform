package blockchain.crypto.sha;

import java.io.IOException;

import static org.jocl.CL.*;

import org.jocl.*;

public class Sha256 extends Sha256Abstract {
    protected cl_mem messageDigest;
    protected int[] result = new int[8];

    public Sha256() {
        this.kernelPath = "src/main/java/blockchain/kernel/Sha256.cl";
        try {
            init();
        } catch (IOException e) {
            System.err.println("Error while initializing Sha256 instance");
            e.printStackTrace();
        }
    }

    public void setData(String input) {
        dataArray = input.toCharArray();
        lth = dataArray.length;
    }

    public String crypt() {
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

        return resultToString(result);
    }
}
