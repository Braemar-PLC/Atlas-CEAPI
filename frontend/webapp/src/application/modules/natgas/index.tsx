import { useEffect } from "react";
import { acquireNatGasStream } from "@/application/subscriptions/natgas-subscriptions-manager";
import { NatGasComponent } from "./natgas.component";
import { useNatGasViewModel } from "./natgas.view-model";


export default function NatGasModule() {
  useEffect(() => {
    let handle: { release(): void } | null = null;

    acquireNatGasStream().then(h => {
      handle = h;
    });

    return () => handle?.release();
  }, []);

  const vm = useNatGasViewModel();

  return (
    <NatGasComponent
      data={vm.rows}
      colDefs={vm.colDefs}
    />
  );
}