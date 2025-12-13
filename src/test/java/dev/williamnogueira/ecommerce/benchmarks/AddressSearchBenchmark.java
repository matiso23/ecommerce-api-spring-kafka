package dev.williamnogueira.ecommerce.benchmarks;

import dev.williamnogueira.ecommerce.domain.address.AddressEntity;
import dev.williamnogueira.ecommerce.domain.address.AddressTypeEnum;
import dev.williamnogueira.ecommerce.domain.shoppingcart.ShoppingCartEntity;
import dev.williamnogueira.ecommerce.domain.customer.CustomerEntity;
import dev.williamnogueira.ecommerce.utils.CustomerTestUtils;
import dev.williamnogueira.ecommerce.utils.AddressTestUtils;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
public class AddressSearchBenchmark {

    private ShoppingCartEntity shoppingCartWithAddresses;
    private static final int NUM_OTHER_ADDRESSES = 10;

    @Setup(Level.Trial)
    public void setup() {
        CustomerEntity customer = CustomerTestUtils.createCustomerEntity();
        List<AddressEntity> addresses = new ArrayList<>();

        // Many irrelevant addresses first (worst case scenario)
        IntStream.range(0, NUM_OTHER_ADDRESSES).forEach(i -> {
            AddressEntity nonRelevant = AddressTestUtils.createAddressEntity();
            nonRelevant.setType(AddressTypeEnum.BILLING);
            addresses.add(nonRelevant);
        });

        // Add the actual shipping address
        AddressEntity shippingAddress = AddressTestUtils.createAddressEntity();
        shippingAddress.setType(AddressTypeEnum.SHIPPING);
        addresses.add(shippingAddress);

        // Add the actual billing address
        AddressEntity billingAddress = AddressTestUtils.createAddressEntity();
        billingAddress.setType(AddressTypeEnum.BILLING);
        addresses.add(billingAddress);

        customer.setAddress(addresses);

        this.shoppingCartWithAddresses = ShoppingCartEntity.builder()
                .customer(customer)
                .items(List.of())
                .build();
    }

    /**
     * BASELINE: Two separate stream passes over the address list.
     * Potential inefficiency if the list is long, as the stream is re-started if SHIPPING is not found early.
     */
    @Benchmark
    public void benchmark_01_OriginalTwoPassStream(Blackhole bh) {
        List<AddressEntity> customerAddresses = shoppingCartWithAddresses.getCustomer().getAddress();

        AddressEntity result = customerAddresses.stream()
                .filter(a -> a.getType().equals(AddressTypeEnum.SHIPPING))
                .findFirst()
                .orElseGet(() -> customerAddresses.stream()
                        .filter(a -> a.getType().equals(AddressTypeEnum.BILLING))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Address not found")));

        bh.consume(result);
    }

    /**
     * OPTIMIZED LOGIC: Single manual iteration for a better performance guarantee.
     */
    @Benchmark
    public void benchmark_02_OptimizedSinglePassForLoop(Blackhole bh) {
        AddressEntity shipping = null;
        AddressEntity billing = null;

        for (AddressEntity a : shoppingCartWithAddresses.getCustomer().getAddress()) {
            if (a.getType().equals(AddressTypeEnum.SHIPPING)) {
                shipping = a;
                // Exit early once SHIPPING is found (best case scenario)
                break;
            }
            if (billing == null && a.getType().equals(AddressTypeEnum.BILLING)) {
                // Store the first found BILLING address as fallback
                billing = a;
            }
        }

        AddressEntity result = (shipping != null) ? shipping :
                (billing != null) ? billing :
                        AddressTestUtils.createAddressEntity();

        bh.consume(result);
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(AddressSearchBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}