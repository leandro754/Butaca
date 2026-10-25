import { brandGeometry } from "./brandGeometry";

function PopcornMark() {
  return (
    <g data-logo-mark>
      <g className="brand-bucket" data-logo-bucket>
        {brandGeometry.bucketPaths.map((path, index) => (
          <path d={path} key={index} />
        ))}
      </g>
      <g className="brand-popcorn">
        {brandGeometry.kernels.map((kernel, index) => (
          <g
            key={index}
            transform={`translate(${kernel.x} ${kernel.y}) rotate(${kernel.rotation}) scale(${kernel.scale})`}
          >
            <path d={brandGeometry.kernelPath} data-logo-popcorn={index} />
          </g>
        ))}
      </g>
    </g>
  );
}
export default PopcornMark;
