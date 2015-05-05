public class CausalMessage {
	Message m;
	int N;
	int W[][];

	public CausalMessage(Message m, int N, int[][] matrix) {
		this.m = m;
		this.N = N;
		W = matrix;
	}

	public int[][] getMatrix() {
		return W;
	}

	public Message getMessage() {
		return m;
	}
}
