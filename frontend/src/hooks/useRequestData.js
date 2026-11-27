import { useCallback, useEffect, useState } from 'react';

/** Holds only the current request, so an earlier filter response cannot overwrite new data. */
export function useRequestData(load) {
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState(null);
  const reload = useCallback(() => setRevision(value => value + 1), []);

  useEffect(() => {
    if (!load) return undefined;
    let active = true;
    Promise.resolve().then(load)
      .then(data => { if (active) setResult({ load, revision, data, error: '' }); })
      .catch(error => { if (active) setResult({ load, revision, data: null, error: error.message }); });
    return () => { active = false; };
  }, [load, revision]);

  const current = load && result?.load === load && result.revision === revision ? result : null;
  return {
    data: current?.data ?? null,
    error: current?.error ?? '',
    loading: Boolean(load && !current),
    reload,
  };
}
