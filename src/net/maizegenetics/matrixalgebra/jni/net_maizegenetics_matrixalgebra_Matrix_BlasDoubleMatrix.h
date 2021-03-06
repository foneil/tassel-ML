/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix */

#ifndef _Included_net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix
#define _Included_net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix
 * Method:    multMatrices
 * Signature: ([DII[DII[DDDZZ)V
 */
JNIEXPORT void JNICALL Java_net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix_multMatrices
  (JNIEnv *, jclass, jdoubleArray, jint, jint, jdoubleArray, jint, jint, jdoubleArray, jdouble, jdouble, jboolean, jboolean);

/*
 * Class:     net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix
 * Method:    solveLSdgelsd
 * Signature: ([DII[DID[I)I
 */
JNIEXPORT jint JNICALL Java_net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix_solveLSdgelsd
  (JNIEnv *, jclass, jdoubleArray, jint, jint, jdoubleArray, jint, jdouble, jintArray);

/*
 * Class:     net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix
 * Method:    solveLSdgelsy
 * Signature: ([DII[DID[I)I
 */
JNIEXPORT jint JNICALL Java_net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix_solveLSdgelsy
  (JNIEnv *, jclass, jdoubleArray, jint, jint, jdoubleArray, jint, jdouble, jintArray);

/*
 * Class:     net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix
 * Method:    singularValueDecompositionDgesvd
 * Signature: (CCII[DI[D[DI[DI)I
 */
JNIEXPORT jint JNICALL Java_net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix_singularValueDecompositionDgesvd
  (JNIEnv *, jclass, jchar, jchar, jint, jint, jdoubleArray, jint, jdoubleArray, jdoubleArray, jint, jdoubleArray, jint);

/*
 * Class:     net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix
 * Method:    singularValueDecompositionDgesdd
 * Signature: (CII[DI[D[DI[DI)I
 */
JNIEXPORT jint JNICALL Java_net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix_singularValueDecompositionDgesdd
  (JNIEnv *, jclass, jchar, jint, jint, jdoubleArray, jint, jdoubleArray, jdoubleArray, jint, jdoubleArray, jint);

/*
 * Class:     net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix
 * Method:    eigenValueSymmetricDecomposition
 * Signature: (I[D[D[D)I
 */
JNIEXPORT jint JNICALL Java_net_maizegenetics_matrixalgebra_Matrix_BlasDoubleMatrix_eigenValueSymmetricDecomposition
  (JNIEnv *, jclass, jint, jdoubleArray, jdoubleArray, jdoubleArray);

#ifdef __cplusplus
}
#endif
#endif
