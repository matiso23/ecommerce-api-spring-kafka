package dev.williamnogueira.ecommerce.benchmarks;

import dev.williamnogueira.ecommerce.domain.product.ProductEntity;
import dev.williamnogueira.ecommerce.domain.product.ProductMapper;
import dev.williamnogueira.ecommerce.domain.product.ProductMapperImpl;
import dev.williamnogueira.ecommerce.domain.product.dto.ProductRequestDTO;
import dev.williamnogueira.ecommerce.domain.product.dto.ProductResponseDTO;
import dev.williamnogueira.ecommerce.utils.ProductTestUtils;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.List;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class ProductMapperBenchmark {

    private ProductMapper productMapper;
    private ProductEntity sourceEntity;
    private ProductRequestDTO sourceRequestDTO;

    private List<ProductEntity> entityList;
    private static final int LIST_SIZE = 100;

    @Setup(Level.Trial)
    public void setup() {
        // Manual instantiation of the MapStruct implementation (bypassing Spring)
        this.productMapper = new ProductMapperImpl();

        this.sourceEntity = ProductTestUtils.createProductEntity();
        this.sourceRequestDTO = ProductTestUtils.createProductRequestDTO();

        this.entityList = IntStream.range(0, LIST_SIZE)
                .mapToObj(i -> {
                    // Create slightly different entities to prevent JIT optimizations
                    ProductEntity p = ProductTestUtils.createProductEntity();
                    p.setId(java.util.UUID.randomUUID());
                    p.setSku(p.getSku() + "_" + i);
                    return p;
                })
                .toList();
    }

    // Benchmarks for Single Mappings

    /**
     * Measures the time for mapping a single ProductEntity to ProductResponseDTO (MapStruct).
     */
    @Benchmark
    public void benchmark_01_EntityToResponseDTO_MapStruct(Blackhole bh) {
        ProductResponseDTO result = productMapper.toResponseDTO(sourceEntity);
        bh.consume(result);
    }

    /**
     * Measures the time for mapping a single ProductRequestDTO to ProductEntity (MapStruct).
     */
    @Benchmark
    public void benchmark_02_RequestDTOToEntity_MapStruct(Blackhole bh) {
        ProductEntity result = productMapper.toEntity(sourceRequestDTO);
        bh.consume(result);
    }

    // Benchmark for Batch Mappings

    /**
     * Measures the time for mapping a list of 100 entities to DTOs using MapStruct.
     */
    @Benchmark
    public void benchmark_03_ListEntityToDTO_MapStruct(Blackhole bh) {
        List<ProductResponseDTO> dtos = this.entityList.stream()
                .map(productMapper::toResponseDTO)
                .toList();

        bh.consume(dtos);
    }

    /**
     * Measures the time for mapping a list of 100 entities to DTOs using manual construction.
     * This serves as the performance baseline for comparison.
     */
    @Benchmark
    public void benchmark_04_ListEntityToDTO_Manual(Blackhole bh) {
        List<ProductResponseDTO> dtos = this.entityList.stream()
                .map(entity -> new ProductResponseDTO(
                        entity.getId(),
                        entity.getSku(),
                        entity.getName(),
                        entity.getLabel(),
                        entity.getCategory().name(),
                        entity.getPrice(),
                        entity.getDiscount(),
                        entity.getStockQuantity(),
                        entity.getInstallments(),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()
                ))
                .toList();

        bh.consume(dtos);
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ProductMapperBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}